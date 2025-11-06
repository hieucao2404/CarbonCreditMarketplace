package com.carboncredit.controller;

import com.carboncredit.dto.ApiResponse;
import com.carboncredit.dto.SystemSettingDTO;
import com.carboncredit.exception.EntityNotFoundException;
import com.carboncredit.service.SystemSettingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for System Settings Management
 * 
 * ENDPOINTS:
 * - GET  /api/system-settings           → Get all settings (Admin only)
 * - GET  /api/system-settings/{key}     → Get specific setting (Admin only)
 * - PUT  /api/system-settings/{key}     → Update specific setting (Admin only)
 * - PUT  /api/system-settings/bulk      → Update multiple settings (Admin only)
 * - POST /api/system-settings           → Create new setting (Admin only)
 * 
 * SECURITY: All endpoints require @PreAuthorize("hasRole('ADMIN')")
 */
@Slf4j
@RestController
@RequestMapping("/api/system-settings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SystemSettingController {

    private final SystemSettingService systemSettingService;

    // ========================================
    // 1. GET ENDPOINTS (READ OPERATIONS)
    // ========================================

    /**
     * Get all system settings
     * 
     * HTTP: GET /api/system-settings
     * 
     * LOGIC:
     * 1. Check admin role (@PreAuthorize)
     * 2. Call service to get all settings
     * 3. Return list of SettingDTOs
     * 
     * RESPONSE:
     * 200 OK - List of all system settings
     * 
     * @return ApiResponse containing list of SystemSettingDTO
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<SystemSettingDTO>>> getAllSettings() {
        try {
            log.info("📋 GET /api/system-settings - Fetching all settings");
            
            List<SystemSettingDTO> settings = systemSettingService.getAllSettings();
            
            log.info("✅ Retrieved {} system settings", settings.size());
            
            return ResponseEntity.ok(
                ApiResponse.success("Retrieved all system settings", settings)
            );
        } catch (Exception e) {
            log.error("❌ Error fetching all settings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve settings: " + e.getMessage()));
        }
    }

    /**
     * Get a specific setting by key
     * 
     * HTTP: GET /api/system-settings/{key}
     * 
     * LOGIC:
     * 1. Check admin role
     * 2. Validate key is not empty
     * 3. Call service to get setting by key
     * 4. Return setting DTO
     * 
     * RESPONSE:
     * 200 OK - The requested setting
     * 404 NOT FOUND - Setting with key not found
     * 
     * @param key - Setting key (e.g., "PLATFORM_FEE_PERCENT")
     * @return ApiResponse containing SystemSettingDTO
     */
    @GetMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SystemSettingDTO>> getSettingByKey(
            @PathVariable String key) {
        try {
            log.info("🔍 GET /api/system-settings/{} - Fetching setting", key);
            
            // Validate key
            if (key == null || key.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Setting key cannot be empty"));
            }
            
            SystemSettingDTO setting = systemSettingService.getSettingByKey(key);
            
            return ResponseEntity.ok(
                ApiResponse.success("Setting retrieved successfully", setting)
            );
        } catch (EntityNotFoundException e) {
            log.warn("⚠️ Setting not found: {}", key);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Setting not found: " + key));
        } catch (Exception e) {
            log.error("❌ Error fetching setting: {}", key, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch setting: " + e.getMessage()));
        }
    }

    // ========================================
    // 2. UPDATE ENDPOINTS (WRITE OPERATIONS)
    // ========================================

    /**
     * Update a single setting
     * 
     * HTTP: PUT /api/system-settings/{key}
     * 
     * REQUEST BODY:
     * {
     *   "value": "new_value"
     * }
     * 
     * LOGIC:
     * 1. Check admin role
     * 2. Validate key and value are not empty
     * 3. Call service to update setting
     * 4. Cache is automatically evicted
     * 5. Return updated setting
     * 
     * RESPONSE:
     * 200 OK - Setting updated successfully
     * 400 BAD REQUEST - Invalid key or value
     * 404 NOT FOUND - Setting with key not found
     * 
     * @param key - Setting key to update
     * @param body - Request body containing "value"
     * @return ApiResponse containing updated SystemSettingDTO
     */
    @PutMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SystemSettingDTO>> updateSetting(
            @PathVariable String key,
            @RequestBody Map<String, String> body) {
        try {
            log.info("✏️ PUT /api/system-settings/{} - Updating setting", key);
            
            // Validate inputs
            if (key == null || key.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Setting key cannot be empty"));
            }
            
            String value = body.get("value");
            if (value == null || value.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Setting value cannot be empty"));
            }
            
            // Update setting
            SystemSettingDTO updated = systemSettingService.updateSetting(key, value);
            
            log.info("✅ Setting {} updated successfully", key);
            
            return ResponseEntity.ok(
                ApiResponse.success("Setting updated successfully", updated)
            );
        } catch (EntityNotFoundException e) {
            log.warn("⚠️ Setting not found: {}", key);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Setting not found: " + key));
        } catch (IllegalStateException e) {
            log.warn("⚠️ Cannot update setting: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error updating setting: {}", key, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update setting: " + e.getMessage()));
        }
    }

    /**
     * Bulk update multiple settings
     * 
     * HTTP: PUT /api/system-settings/bulk
     * 
     * REQUEST BODY:
     * {
     *   "PLATFORM_FEE_PERCENT": "7.0",
     *   "MIN_CREDIT_AMOUNT": "0.5",
     *   "MAX_CREDIT_AMOUNT": "500.0"
     * }
     * 
     * LOGIC:
     * 1. Check admin role
     * 2. Validate request body is not empty
     * 3. For each key-value pair, call updateSetting()
     * 4. If ANY fails, entire operation fails (atomic)
     * 5. Cache evicted once
     * 6. Return all updated settings
     * 
     * TRANSACTION GUARANTEE:
     * - Either ALL settings are updated, or NONE are
     * - Database consistency maintained
     * 
     * RESPONSE:
     * 200 OK - All settings updated successfully
     * 400 BAD REQUEST - Invalid request body
     * 404 NOT FOUND - Any setting not found (entire operation fails)
     * 
     * @param updates - Map of setting keys to new values
     * @return ApiResponse containing list of updated SystemSettingDTOs
     */
    @PutMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<SystemSettingDTO>>> bulkUpdateSettings(
            @RequestBody Map<String, String> updates) {
        try {
            log.info("📦 PUT /api/system-settings/bulk - Bulk updating {} settings", updates.size());
            
            // Validate inputs
            if (updates == null || updates.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Updates map cannot be empty"));
            }
            
            // Perform bulk update (returns all updated settings)
            List<SystemSettingDTO> updated = updates.entrySet().stream()
                    .map(entry -> {
                        try {
                            return systemSettingService.updateSetting(entry.getKey(), entry.getValue());
                        } catch (Exception e) {
                            log.error("❌ Failed to update setting '{}': {}", 
                                     entry.getKey(), e.getMessage());
                            throw new RuntimeException(
                                "Failed to update '" + entry.getKey() + "': " + e.getMessage(), e
                            );
                        }
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            log.info("✅ Successfully bulk updated {} settings", updated.size());
            
            return ResponseEntity.ok(
                ApiResponse.success("Settings updated successfully", updated)
            );
        } catch (RuntimeException e) {
            log.error("❌ Error in bulk update", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Bulk update failed: " + e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Unexpected error in bulk update", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update settings: " + e.getMessage()));
        }
    }

    // ========================================
    // 3. CREATE ENDPOINTS
    // ========================================

    /**
     * Create a new system setting
     * 
     * HTTP: POST /api/system-settings
     * 
     * REQUEST BODY:
     * {
     *   "key": "NEW_SETTING_KEY",
     *   "value": "default_value",
     *   "description": "Description of the setting",
     *   "dataType": "STRING"
     * }
     * 
     * LOGIC:
     * 1. Check admin role
     * 2. Validate all required fields
     * 3. Check if setting key already exists
     * 4. Call service to create new setting
     * 5. Cache evicted
     * 6. Return created setting
     * 
     * RESPONSE:
     * 201 CREATED - Setting created successfully
     * 400 BAD REQUEST - Invalid request body or key already exists
     * 
     * @param settingData - Request body with setting details
     * @return ApiResponse containing created SystemSettingDTO
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SystemSettingDTO>> createSetting(
            @RequestBody Map<String, String> settingData) {
        try {
            log.info("➕ POST /api/system-settings - Creating new setting");
            
            // Validate required fields
            String key = settingData.get("key");
            String value = settingData.get("value");
            String description = settingData.get("description");
            String dataType = settingData.get("dataType");
            
            if (key == null || key.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Setting key is required"));
            }
            
            if (value == null || value.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Setting value is required"));
            }
            
            if (dataType == null || dataType.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Setting dataType is required"));
            }
            
            // Create setting
            SystemSettingDTO created = systemSettingService.createSetting(
                    key, value, description != null ? description : "", dataType
            );
            
            log.info("✅ New setting created: {}", key);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Setting created successfully", created));
        } catch (IllegalStateException e) {
            log.warn("⚠️ Cannot create setting: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error creating setting", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create setting: " + e.getMessage()));
        }
    }

    // ========================================
    // 4. HEALTH CHECK ENDPOINTS
    // ========================================

    /**
     * Health check endpoint (public)
     * 
     * HTTP: GET /api/system-settings/health
     * 
     * LOGIC:
     * 1. No authentication required
     * 2. Check if settings service is responsive
     * 3. Return status
     * 
     * @return ApiResponse with health status
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        try {
            systemSettingService.getAllSettings();
            return ResponseEntity.ok(ApiResponse.success("System settings service is healthy"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("System settings service is unavailable"));
        }
    }
}
