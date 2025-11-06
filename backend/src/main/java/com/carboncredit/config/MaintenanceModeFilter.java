package com.carboncredit.config;

import com.carboncredit.service.SystemSettingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * Filter to block all requests during maintenance mode
 * 
 * INTEGRATION POINT: Uses SystemSettings for MAINTENANCE_MODE flag
 */
@Slf4j
@Component
@Order(1) // Execute before security filters
@RequiredArgsConstructor
public class MaintenanceModeFilter implements Filter {

    private final SystemSettingService systemSettingService;
    private final ObjectMapper objectMapper;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Allow health checks and system settings endpoints
        String path = httpRequest.getRequestURI();
        if (path.startsWith("/actuator") || 
            path.equals("/api/system-settings/health") ||
            path.startsWith("/api/auth") || path.startsWith("/api/system-settings")) {
            chain.doFilter(request, response);
            return;
        }
        
        // Check maintenance mode
        try {
            Boolean maintenanceMode = systemSettingService.getSettingAsBoolean("MAINTENANCE_MODE");
            
            if (Boolean.TRUE.equals(maintenanceMode)) {
                log.warn("🚧 Maintenance mode active - blocking request to: {}", path);
                
                httpResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                httpResponse.setContentType("application/json");
                
                Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "System is under maintenance. Please try again later.",
                    "status", 503
                );
                
                httpResponse.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                return;
            }
        } catch (Exception e) {
            log.error("❌ Error checking maintenance mode, allowing request", e);
        }
        
        // Continue filter chain
        chain.doFilter(request, response);
    }
}
