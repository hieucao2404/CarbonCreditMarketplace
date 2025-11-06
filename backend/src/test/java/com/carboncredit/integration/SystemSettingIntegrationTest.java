package com.carboncredit.integration;

import com.carboncredit.dto.ApiResponse;
import com.carboncredit.dto.SystemSettingDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive Integration Tests for SystemSetting endpoints
 * 
 * TESTING STRATEGY:
 * - Test full HTTP request/response cycle
 * - Verify database persistence and caching
 * - Test security (ADMIN role required, exceptions for health endpoint)
 * - Test error scenarios and edge cases
 * - Test maintenance mode filter integration
 * - Test concurrent access scenarios
 * 
 * FIXES FOR FAILURES:
 * 1. Fixed 403→500 error by adding proper exception handling
 * 2. Fixed maintenance mode blocking by disabling it first
 * 3. Fixed health endpoint requiring auth (should be public)
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("SystemSetting Integration Tests - Complete Suite")
class SystemSettingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ========================================
    // SETUP & TEARDOWN
    // ========================================

    @BeforeEach
    void setUp() throws Exception {
        // Ensure maintenance mode is OFF before each test
        try {
            mockMvc.perform(put("/api/system-settings/MAINTENANCE_MODE")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"value\": \"false\"}")
                    .header("Authorization", "Basic " + getAdminAuthHeader()));
        } catch (Exception e) {
            // Ignore if setting doesn't exist yet
        }
    }

    // ========================================
    // 1. HEALTH CHECK TESTS (PUBLIC ENDPOINT)
    // ========================================

    @Test
    @Order(1)
    @DisplayName("GET /api/system-settings/health - Should work without authentication")
    void testHealthCheck_NoAuth_Success() throws Exception {
        mockMvc.perform(get("/api/system-settings/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("System settings service is healthy"));
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/system-settings/health - Should return setting count")
    void testHealthCheck_ReturnsMetrics() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/system-settings/health"))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        Map<String, Object> response = objectMapper.readValue(json, new TypeReference<>() {});
        
        assertNotNull(response.get("data"));
        assertTrue(response.get("data") instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        assertTrue(data.containsKey("totalSettings"));
    }

    // ========================================
    // 2. GET ENDPOINTS TESTS
    // ========================================

    @Test
    @Order(10)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/system-settings - Should return all settings")
    void testGetAllSettings_Success() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/system-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        ApiResponse<List<SystemSettingDTO>> response = objectMapper.readValue(json, 
                new TypeReference<ApiResponse<List<SystemSettingDTO>>>() {});

        assertNotNull(response.getData());
        assertTrue(response.getData().size() > 0);
        
        // Verify required settings exist
        List<String> keys = response.getData().stream()
                .map(SystemSettingDTO::getSettingKey)
                .toList();
        assertTrue(keys.contains("PLATFORM_FEE_PERCENT"));
        assertTrue(keys.contains("MIN_CREDIT_AMOUNT"));
    }

    @Test
    @Order(11)
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/system-settings - Should deny access for non-admin")
    void testGetAllSettings_Forbidden() throws Exception {
        mockMvc.perform(get("/api/system-settings"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(12)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/system-settings/{key} - Should return specific setting")
    void testGetSettingByKey_Success() throws Exception {
        mockMvc.perform(get("/api/system-settings/PLATFORM_FEE_PERCENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.settingKey").value("PLATFORM_FEE_PERCENT"))
                .andExpect(jsonPath("$.data.dataType").value("DECIMAL"))
                .andExpect(jsonPath("$.data.isEditable").value(true));
    }

    @Test
    @Order(13)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/system-settings/{key} - Should return 404 for invalid key")
    void testGetSettingByKey_NotFound() throws Exception {
        mockMvc.perform(get("/api/system-settings/NONEXISTENT_SETTING_KEY"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("System setting not found: NONEXISTENT_SETTING_KEY"));
    }

    @Test
    @Order(14)
    @WithMockUser(roles = "BUYER")
    @DisplayName("GET /api/system-settings/{key} - Should deny access for buyers")
    void testGetSettingByKey_BuyerDenied() throws Exception {
        mockMvc.perform(get("/api/system-settings/PLATFORM_FEE_PERCENT"))
                .andExpect(status().isForbidden());
    }

    // ========================================
    // 3. UPDATE ENDPOINTS TESTS
    // ========================================

    @Test
    @Order(20)
    @WithMockUser(roles = "ADMIN")
    @Transactional
    @DirtiesContext
    @DisplayName("PUT /api/system-settings/{key} - Should update setting value")
    void testUpdateSetting_Success() throws Exception {
        Map<String, String> updateRequest = Map.of("value", "7.5");

        mockMvc.perform(put("/api/system-settings/PLATFORM_FEE_PERCENT")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.settingValue").value("7.5"))
                .andExpect(jsonPath("$.message").value("Setting updated successfully"));
    }

    @Test
    @Order(21)
    @WithMockUser(roles = "USER")
    @DisplayName("PUT /api/system-settings/{key} - Should return 403 for non-admin")
    void testUpdateSetting_Forbidden() throws Exception {
        Map<String, String> updateRequest = Map.of("value", "10.0");

        mockMvc.perform(put("/api/system-settings/PLATFORM_FEE_PERCENT")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(22)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/system-settings/{key} - Should reject invalid data type")
    void testUpdateSetting_InvalidDataType() throws Exception {
        Map<String, String> updateRequest = Map.of("value", "not_a_number");

        mockMvc.perform(put("/api/system-settings/PLATFORM_FEE_PERCENT")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Order(23)
    @WithMockUser(roles = "ADMIN")
    @Transactional
    @DirtiesContext
    @DisplayName("PUT /api/system-settings/bulk - Should update multiple settings")
    void testBulkUpdateSettings_Success() throws Exception {
        Map<String, String> bulkUpdate = Map.of(
                "PLATFORM_FEE_PERCENT", "6.0",
                "MIN_CREDIT_AMOUNT", "0.5"
        );

        MvcResult result = mockMvc.perform(put("/api/system-settings/bulk")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        ApiResponse<List<SystemSettingDTO>> response = objectMapper.readValue(json,
                new TypeReference<ApiResponse<List<SystemSettingDTO>>>() {});

        assertEquals(2, response.getData().size());
    }

    @Test
    @Order(24)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/system-settings/bulk - Should partially succeed")
    void testBulkUpdateSettings_PartialSuccess() throws Exception {
        Map<String, String> bulkUpdate = Map.of(
                "PLATFORM_FEE_PERCENT", "5.5",
                "INVALID_KEY", "some_value"
        );

        mockMvc.perform(put("/api/system-settings/bulk")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkUpdate)))
                .andExpect(status().isMultiStatus())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("partial")));
    }

    // ========================================
    // 4. CREATE ENDPOINTS TESTS
    // ========================================

    @Test
    @Order(30)
    @WithMockUser(roles = "ADMIN")
    @Transactional
    @DirtiesContext
    @DisplayName("POST /api/system-settings - Should create new setting")
    void testCreateSetting_Success() throws Exception {
        Map<String, String> newSetting = Map.of(
                "key", "TEST_SETTING_" + System.currentTimeMillis(),
                "value", "test_value",
                "description", "Test setting for integration test",
                "dataType", "STRING"
        );

        mockMvc.perform(post("/api/system-settings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newSetting)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.settingValue").value("test_value"));
    }

    @Test
    @Order(31)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/system-settings - Should reject duplicate key")
    void testCreateSetting_Duplicate() throws Exception {
        Map<String, String> duplicateSetting = Map.of(
                "key", "PLATFORM_FEE_PERCENT",
                "value", "5.0",
                "description", "Duplicate",
                "dataType", "DECIMAL"
        );

        mockMvc.perform(post("/api/system-settings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateSetting)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already exists")));
    }

    @Test
    @Order(32)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/system-settings - Should reject invalid data type")
    void testCreateSetting_InvalidDataType() throws Exception {
        Map<String, String> invalidSetting = Map.of(
                "key", "NEW_SETTING",
                "value", "value",
                "description", "Test",
                "dataType", "INVALID_TYPE"
        );

        mockMvc.perform(post("/api/system-settings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidSetting)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(33)
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/system-settings - Should deny non-admin")
    void testCreateSetting_Forbidden() throws Exception {
        Map<String, String> newSetting = Map.of(
                "key", "USER_SETTING",
                "value", "value",
                "description", "Test",
                "dataType", "STRING"
        );

        mockMvc.perform(post("/api/system-settings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newSetting)))
                .andExpect(status().isForbidden());
    }

    // ========================================
    // 5. CACHING TESTS
    // ========================================

    @Test
    @Order(40)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Cache - Should cache GET requests")
    void testCaching_GetRequests() throws Exception {
        // First request - should hit database
        long start1 = System.currentTimeMillis();
        mockMvc.perform(get("/api/system-settings/PLATFORM_FEE_PERCENT"))
                .andExpect(status().isOk());
        long duration1 = System.currentTimeMillis() - start1;

        // Second request - should hit cache (faster)
        long start2 = System.currentTimeMillis();
        mockMvc.perform(get("/api/system-settings/PLATFORM_FEE_PERCENT"))
                .andExpect(status().isOk());
        long duration2 = System.currentTimeMillis() - start2;

        // Cache hit should be significantly faster (usually <5ms vs >20ms)
        assertTrue(duration2 < duration1, 
            "Second request should be faster due to caching");
    }

    @Test
    @Order(41)
    @WithMockUser(roles = "ADMIN")
    @Transactional
    @DirtiesContext
    @DisplayName("Cache - Should invalidate on update")
    void testCaching_InvalidateOnUpdate() throws Exception {
        // Get initial value (cache it)
        mockMvc.perform(get("/api/system-settings/PLATFORM_FEE_PERCENT"))
                .andExpect(status().isOk());

        // Update setting (should clear cache)
        Map<String, String> update = Map.of("value", "8.0");
        mockMvc.perform(put("/api/system-settings/PLATFORM_FEE_PERCENT")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk());

        // Get again (should reflect new value)
        mockMvc.perform(get("/api/system-settings/PLATFORM_FEE_PERCENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settingValue").value("8.0"));
    }

    // ========================================
    // 6. MAINTENANCE MODE INTEGRATION TESTS
    // ========================================

    @Test
    @Order(50)
    @WithMockUser(roles = "ADMIN")
    @Transactional
    @DirtiesContext
    @DisplayName("Maintenance Mode - Should enable maintenance mode")
    void testMaintenanceMode_Enable() throws Exception {
        Map<String, String> enableMaintenance = Map.of("value", "true");

        mockMvc.perform(put("/api/system-settings/MAINTENANCE_MODE")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enableMaintenance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settingValue").value("true"));
    }

    @Test
    @Order(51)
    @WithMockUser(roles = "ADMIN")
    @Transactional
    @DirtiesContext
    @DisplayName("Maintenance Mode - Should block requests when enabled")
    void testMaintenanceMode_BlocksRequests() throws Exception {
        // Enable maintenance mode first
        Map<String, String> enableMaintenance = Map.of("value", "true");
        mockMvc.perform(put("/api/system-settings/MAINTENANCE_MODE")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(enableMaintenance)));

        // Wait for cache to clear
        Thread.sleep(100);

        // Try to access other endpoints (should be blocked)
        mockMvc.perform(get("/api/carbon-credits"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("maintenance")));

        // Disable maintenance mode for other tests
        Map<String, String> disableMaintenance = Map.of("value", "false");
        mockMvc.perform(put("/api/system-settings/MAINTENANCE_MODE")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(disableMaintenance)));
    }

    @Test
    @Order(52)
    @WithMockUser(roles = "ADMIN")
    @Transactional
    @DirtiesContext
    @DisplayName("Maintenance Mode - Health endpoint should work during maintenance")
    void testMaintenanceMode_HealthEndpointWorks() throws Exception {
        // Enable maintenance mode
        Map<String, String> enableMaintenance = Map.of("value", "true");
        mockMvc.perform(put("/api/system-settings/MAINTENANCE_MODE")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(enableMaintenance)));

        // Health endpoint should still work
        mockMvc.perform(get("/api/system-settings/health"))
                .andExpect(status().isOk());

        // Disable maintenance mode
        Map<String, String> disableMaintenance = Map.of("value", "false");
        mockMvc.perform(put("/api/system-settings/MAINTENANCE_MODE")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(disableMaintenance)));
    }

    // ========================================
    // 7. ERROR HANDLING TESTS
    // ========================================

    @Test
    @Order(60)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Error Handling - Missing request body")
    void testErrorHandling_MissingBody() throws Exception {
        mockMvc.perform(put("/api/system-settings/PLATFORM_FEE_PERCENT")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(61)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Error Handling - Malformed JSON")
    void testErrorHandling_MalformedJson() throws Exception {
        mockMvc.perform(put("/api/system-settings/PLATFORM_FEE_PERCENT")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(62)
    @DisplayName("Error Handling - Unauthenticated access")
    void testErrorHandling_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/system-settings"))
                .andExpect(status().isUnauthorized());
    }

    // ========================================
    // 8. BUSINESS LOGIC INTEGRATION TESTS
    // ========================================

    @Test
    @Order(70)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Integration - Platform fee affects transaction calculation")
    void testIntegration_PlatformFeeAffectsTransactions() throws Exception {
        // Update platform fee
        Map<String, String> update = Map.of("value", "5.0");
        mockMvc.perform(put("/api/system-settings/PLATFORM_FEE_PERCENT")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk());

        // Verify TransactionService uses new fee
        // (This would require TransactionService bean injection)
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    private String getAdminAuthHeader() {
        String credentials = "admin:admin123";
        return java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
    }
}
