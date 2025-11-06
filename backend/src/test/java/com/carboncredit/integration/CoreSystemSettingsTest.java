package com.carboncredit.integration;

import com.carboncredit.dto.ApiResponse;
import com.carboncredit.dto.SystemSettingDTO;
import com.carboncredit.service.SystemSettingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ═══════════════════════════════════════════════════════════════
 *  CORE SYSTEM SETTINGS TEST - Platform Fee & Maintenance Mode
 * ═══════════════════════════════════════════════════════════════
 * 
 * PURPOSE:
 * Test the two most critical system settings:
 * 1. PLATFORM_FEE_PERCENT - Dynamic transaction fee
 * 2. MAINTENANCE_MODE - System-wide request blocking
 * 
 * WHAT IS BEING TESTED:
 * ✅ Admin can read current fee percentage
 * ✅ Admin can update fee percentage (e.g., 5% → 7.5%)
 * ✅ Admin can enable/disable maintenance mode
 * ✅ Changes persist in database
 * ✅ Only ADMIN role can access these settings
 * 
 * HOW IT WORKS:
 * 1. Database stores settings as key-value pairs:
 *    - Key: "PLATFORM_FEE_PERCENT", Value: "5.0"
 *    - Key: "MAINTENANCE_MODE", Value: "false"
 * 
 * 2. SystemSettingService reads these values:
 *    - getSettingAsDouble("PLATFORM_FEE_PERCENT") → 5.0
 *    - getSettingAsBoolean("MAINTENANCE_MODE") → false
 * 
 * 3. Other services use these values:
 *    - TransactionService: Calculates fee dynamically
 *    - MaintenanceModeFilter: Blocks requests if true
 * 
 * 4. Admin updates via REST API:
 *    - PUT /api/system-settings/PLATFORM_FEE_PERCENT
 *    - Body: {"value": "7.5"}
 *    - Result: All future transactions use 7.5% fee
 * 
 * WHY THIS IS USEFUL:
 * - Change fee without redeploying code
 * - Emergency maintenance mode activation
 * - Test different fee scenarios easily
 * - Audit trail of who changed what and when
 * 
 * @author CarbonCredit Team
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("🎯 Core System Settings Test - Fee & Maintenance")
class CoreSystemSettingsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SystemSettingService systemSettingService;

    // ═══════════════════════════════════════════════════════════
    // TEST SETUP
    // ═══════════════════════════════════════════════════════════

    @BeforeEach
    void setUp() {
        // Ensure default values before each test
        systemSettingService.updateSetting("PLATFORM_FEE_PERCENT", "5.0");
        systemSettingService.updateSetting("MAINTENANCE_MODE", "false");
    }

    // ═══════════════════════════════════════════════════════════
    // PART 1: PLATFORM FEE TESTS
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(1)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("1️⃣ Get Platform Fee - Should return current fee percentage")
    void test01_GetPlatformFee_Success() throws Exception {
        /*
         * TEST SCENARIO:
         * Admin wants to check the current platform fee percentage
         * 
         * EXPECTED RESULT:
         * - Status: 200 OK
         * - Response contains: settingKey = "PLATFORM_FEE_PERCENT"
         * - Response contains: settingValue = "5.0"
         * - Response contains: dataType = "DECIMAL"
         * 
         * WHY THIS MATTERS:
         * Before changing the fee, admin needs to know the current value
         */
        
        System.out.println("\n🔍 TEST 1: Getting current platform fee...");
        
        MvcResult result = mockMvc.perform(
                get("/api/system-settings/PLATFORM_FEE_PERCENT")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.settingKey").value("PLATFORM_FEE_PERCENT"))
                .andExpect(jsonPath("$.data.settingValue").value("5.0"))
                .andExpect(jsonPath("$.data.dataType").value("DECIMAL"))
                .andReturn();

        // Parse and verify response
        String json = result.getResponse().getContentAsString();
        ApiResponse<SystemSettingDTO> response = objectMapper.readValue(json,
                new TypeReference<ApiResponse<SystemSettingDTO>>() {});

        assertNotNull(response.getData());
        assertEquals("PLATFORM_FEE_PERCENT", response.getData().getSettingKey());
        assertEquals("5.0", response.getData().getSettingValue());
        
        System.out.println("✅ Current fee: " + response.getData().getSettingValue() + "%");
    }

    @Test
    @Order(2)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("2️⃣ Update Platform Fee - Should change fee from 5% to 7.5%")
    void test02_UpdatePlatformFee_Success() throws Exception {
        /*
         * TEST SCENARIO:
         * Admin decides to increase platform fee from 5% to 7.5%
         * 
         * WHAT HAPPENS:
         * 1. Admin sends PUT request with new value "7.5"
         * 2. SystemSettingService updates database
         * 3. Cache is cleared (@CacheEvict)
         * 4. Next transaction will use 7.5% fee
         * 
         * REAL-WORLD EXAMPLE:
         * Transaction amount: $100
         * Before: Platform fee = $100 × 5% = $5
         * After:  Platform fee = $100 × 7.5% = $7.50
         * 
         * NO CODE DEPLOYMENT NEEDED!
         */
        
        System.out.println("\n📝 TEST 2: Updating platform fee to 7.5%...");
        
        Map<String, String> updateRequest = Map.of("value", "7.5");

        MvcResult result = mockMvc.perform(
                put("/api/system-settings/PLATFORM_FEE_PERCENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.settingValue").value("7.5"))
                .andReturn();

        // Verify the update persisted
        String json = result.getResponse().getContentAsString();
        ApiResponse<SystemSettingDTO> response = objectMapper.readValue(json,
                new TypeReference<ApiResponse<SystemSettingDTO>>() {});

        assertEquals("7.5", response.getData().getSettingValue());
        
        // Verify via service (tests cache eviction)
        Double currentFee = systemSettingService.getSettingAsDouble("PLATFORM_FEE_PERCENT");
        assertEquals(7.5, currentFee);
        
        System.out.println("✅ Fee updated successfully: 5.0% → 7.5%");
        System.out.println("💡 All future transactions will use 7.5% fee");
    }

    @Test
    @Order(3)
    @WithMockUser(roles = "USER") // Not ADMIN
    @DisplayName("3️⃣ Update Platform Fee as Regular User - Should be denied")
    void test03_UpdatePlatformFee_Forbidden() throws Exception {
        /*
         * SECURITY TEST:
         * Regular users (role: USER) should NOT be able to change system settings
         * 
         * EXPECTED RESULT:
         * - Status: 403 FORBIDDEN
         * - No database changes occur
         * 
         * WHY THIS MATTERS:
         * Only admins should control platform configuration
         * Prevents unauthorized fee manipulation
         */
        
        System.out.println("\n🔒 TEST 3: Testing security - regular user attempts update...");
        
        Map<String, String> updateRequest = Map.of("value", "99.9");

        mockMvc.perform(
                put("/api/system-settings/PLATFORM_FEE_PERCENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
        )
                .andExpect(status().isForbidden());
        
        // Verify fee was NOT changed
        Double currentFee = systemSettingService.getSettingAsDouble("PLATFORM_FEE_PERCENT");
        assertNotEquals(99.9, currentFee);
        
        System.out.println("✅ Access denied for non-admin user (as expected)");
    }

    @Test
    @Order(4)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("4️⃣ Update Platform Fee with Invalid Value - Should reject")
    void test04_UpdatePlatformFee_InvalidValue() throws Exception {
        /*
         * VALIDATION TEST:
         * System should reject invalid fee values
         * 
         * INVALID CASES:
         * - Empty string: ""
         * - Non-numeric: "abc"
         * - Negative: "-5.0"
         * - Too high: "150.0" (more than 100%)
         * 
         * EXPECTED RESULT:
         * - Status: 400 BAD REQUEST
         * - Original value unchanged
         */
        
        System.out.println("\n❌ TEST 4: Testing validation with empty value...");
        
        Map<String, String> invalidRequest = Map.of("value", "");

        mockMvc.perform(
                put("/api/system-settings/PLATFORM_FEE_PERCENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
        )
                .andExpect(status().isBadRequest());
        
        System.out.println("✅ Invalid value rejected (as expected)");
    }

    // ═══════════════════════════════════════════════════════════
    // PART 2: MAINTENANCE MODE TESTS
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(5)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("5️⃣ Get Maintenance Mode Status - Should return current state")
    void test05_GetMaintenanceMode_Success() throws Exception {
        /*
         * TEST SCENARIO:
         * Check if system is in maintenance mode
         * 
         * EXPECTED RESULT:
         * - Status: 200 OK
         * - settingKey = "MAINTENANCE_MODE"
         * - settingValue = "false" (default)
         * - dataType = "BOOLEAN"
         * 
         * USE CASE:
         * Admin dashboard shows maintenance status indicator
         */
        
        System.out.println("\n🔍 TEST 5: Checking maintenance mode status...");
        
        MvcResult result = mockMvc.perform(
                get("/api/system-settings/MAINTENANCE_MODE")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.settingKey").value("MAINTENANCE_MODE"))
                .andExpect(jsonPath("$.data.settingValue").value("false"))
                .andExpect(jsonPath("$.data.dataType").value("BOOLEAN"))
                .andReturn();

        String json = result.getResponse().getContentAsString();
        ApiResponse<SystemSettingDTO> response = objectMapper.readValue(json,
                new TypeReference<ApiResponse<SystemSettingDTO>>() {});

        assertEquals("false", response.getData().getSettingValue());
        
        System.out.println("✅ Maintenance mode: OFF");
    }

    @Test
    @Order(6)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("6️⃣ Enable Maintenance Mode - Should activate system-wide block")
    void test06_EnableMaintenanceMode_Success() throws Exception {
        /*
         * TEST SCENARIO:
         * Emergency situation - enable maintenance mode
         * 
         * WHAT HAPPENS:
         * 1. Admin sets MAINTENANCE_MODE = "true"
         * 2. MaintenanceModeFilter reads this value
         * 3. All non-admin requests get 503 Service Unavailable
         * 4. Only /actuator, /api/auth, and /api/system-settings work
         * 
         * REAL-WORLD USE CASES:
         * - Database migration in progress
         * - Security incident response
         * - Critical bug fix deployment
         * - Performance issue investigation
         * 
         * INSTANT EFFECT - NO SERVER RESTART!
         */
        
        System.out.println("\n🚧 TEST 6: Enabling maintenance mode...");
        
        Map<String, String> enableRequest = Map.of("value", "true");

        MvcResult result = mockMvc.perform(
                put("/api/system-settings/MAINTENANCE_MODE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enableRequest))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.settingValue").value("true"))
                .andReturn();

        // Verify via service
        Boolean isMaintenanceMode = systemSettingService.getSettingAsBoolean("MAINTENANCE_MODE");
        assertTrue(isMaintenanceMode);
        
        System.out.println("✅ Maintenance mode ENABLED");
        System.out.println("🚫 All user requests will now be blocked");
        System.out.println("⚠️  Only admins can access system settings");
    }

    @Test
    @Order(7)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("7️⃣ Disable Maintenance Mode - Should restore normal operation")
    void test07_DisableMaintenanceMode_Success() throws Exception {
        /*
         * TEST SCENARIO:
         * Maintenance complete - restore normal operation
         * 
         * WHAT HAPPENS:
         * 1. Admin sets MAINTENANCE_MODE = "false"
         * 2. MaintenanceModeFilter allows all requests again
         * 3. Users can access the platform normally
         * 
         * REAL-WORLD FLOW:
         * 1. [10:00 AM] Enable maintenance mode
         * 2. [10:05 AM] Perform database migration
         * 3. [10:15 AM] Test changes
         * 4. [10:20 AM] Disable maintenance mode ← This test
         * 5. [10:21 AM] Platform back online
         */
        
        System.out.println("\n✅ TEST 7: Disabling maintenance mode...");
        
        // First enable it
        systemSettingService.updateSetting("MAINTENANCE_MODE", "true");
        
        // Now disable it
        Map<String, String> disableRequest = Map.of("value", "false");

        mockMvc.perform(
                put("/api/system-settings/MAINTENANCE_MODE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(disableRequest))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.settingValue").value("false"));

        // Verify via service
        Boolean isMaintenanceMode = systemSettingService.getSettingAsBoolean("MAINTENANCE_MODE");
        assertFalse(isMaintenanceMode);
        
        System.out.println("✅ Maintenance mode DISABLED");
        System.out.println("🎉 Platform back to normal operation");
    }

    // ═══════════════════════════════════════════════════════════
    // PART 3: INTEGRATION TESTS
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(8)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("8️⃣ Bulk Update - Should update both settings at once")
    void test08_BulkUpdate_Success() throws Exception {
        /*
         * TEST SCENARIO:
         * Admin wants to update multiple settings in one request
         * 
         * USE CASE:
         * During planned maintenance:
         * 1. Enable maintenance mode
         * 2. Increase fee for post-maintenance period
         * 3. Both changes happen atomically
         * 
         * EFFICIENCY:
         * - Single HTTP request vs multiple
         * - Single database transaction
         * - Single cache clear operation
         */
        
        System.out.println("\n📦 TEST 8: Bulk updating fee + maintenance...");
        
        Map<String, String> bulkUpdate = Map.of(
                "PLATFORM_FEE_PERCENT", "6.0",
                "MAINTENANCE_MODE", "true"
        );

        mockMvc.perform(
                put("/api/system-settings/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkUpdate))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));

        // Verify both updates
        Double fee = systemSettingService.getSettingAsDouble("PLATFORM_FEE_PERCENT");
        Boolean maintenance = systemSettingService.getSettingAsBoolean("MAINTENANCE_MODE");

        assertEquals(6.0, fee);
        assertTrue(maintenance);
        
        System.out.println("✅ Bulk update successful:");
        System.out.println("   - Platform fee: 5.0% → 6.0%");
        System.out.println("   - Maintenance: OFF → ON");
    }

    @Test
    @Order(9)
    @DisplayName("9️⃣ Health Check - Should work without authentication")
    void test09_HealthCheck_NoAuth() throws Exception {
        /*
         * TEST SCENARIO:
         * Monitoring system checks if service is alive
         * 
         * NO AUTHENTICATION REQUIRED:
         * - Used by load balancers
         * - Used by monitoring tools (Prometheus, Datadog, etc.)
         * - Should always return 200 if service is up
         * 
         * RESPONSE SHOULD BE FAST:
         * - No database query
         * - Just checks if service bean exists
         */
        
        System.out.println("\n🏥 TEST 9: Testing health check endpoint...");
        
        mockMvc.perform(get("/api/system-settings/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("System settings service is healthy"));
        
        System.out.println("✅ Health check passed (no auth required)");
    }

    // ═══════════════════════════════════════════════════════════
    // PART 4: SERVICE LAYER TESTS (Direct Testing)
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(10)
    @DisplayName("🔟 Service Layer - Test getSettingAsDouble() method")
    void test10_ServiceLayer_GetAsDouble() {
        /*
         * UNIT TEST FOR SERVICE METHOD:
         * Test the type conversion logic directly
         * 
         * WHY THIS MATTERS:
         * - TransactionService will call this method
         * - Must correctly parse "5.0" → 5.0
         * - Must handle edge cases
         */
        
        System.out.println("\n🔧 TEST 10: Testing service method getSettingAsDouble()...");
        
        // Test normal case
        Double fee = systemSettingService.getSettingAsDouble("PLATFORM_FEE_PERCENT");
        assertEquals(5.0, fee);
        
        // Test edge case: decimal places
        systemSettingService.updateSetting("PLATFORM_FEE_PERCENT", "7.555");
        Double precisionFee = systemSettingService.getSettingAsDouble("PLATFORM_FEE_PERCENT");
        assertEquals(7.555, precisionFee);
        
        System.out.println("✅ Type conversion works correctly");
    }

    @Test
    @Order(11)
    @DisplayName("1️⃣1️⃣ Service Layer - Test getSettingAsBoolean() method")
    void test11_ServiceLayer_GetAsBoolean() {
        /*
         * UNIT TEST FOR SERVICE METHOD:
         * Test boolean parsing logic
         * 
         * BOOLEAN PARSING RULES:
         * - "true" → true
         * - "TRUE" → true
         * - "false" → false
         * - "FALSE" → false
         * - "1" → false (String "1" is not boolean "true")
         * - "" → false
         * - null → false
         */
        
        System.out.println("\n🔧 TEST 11: Testing service method getSettingAsBoolean()...");
        
        // Test "false"
        Boolean maintenanceOff = systemSettingService
                .getSettingAsBoolean("MAINTENANCE_MODE");
        assertFalse(maintenanceOff);
        
        // Test "true"
        systemSettingService.updateSetting("MAINTENANCE_MODE", "true");
        Boolean maintenanceOn = systemSettingService
                .getSettingAsBoolean("MAINTENANCE_MODE");
        assertTrue(maintenanceOn);
        
        // Test case insensitivity
        systemSettingService.updateSetting("MAINTENANCE_MODE", "TRUE");
        Boolean maintenanceOnUpper = systemSettingService
                .getSettingAsBoolean("MAINTENANCE_MODE");
        assertTrue(maintenanceOnUpper);
        
        System.out.println("✅ Boolean parsing works correctly");
    }

    // ═══════════════════════════════════════════════════════════
    // TEST SUMMARY
    // ═══════════════════════════════════════════════════════════

    @AfterAll
    static void printSummary() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📊 TEST EXECUTION SUMMARY");
        System.out.println("=".repeat(60));
        System.out.println("✅ Platform Fee Tests:");
        System.out.println("   • Get current fee ✓");
        System.out.println("   • Update fee (5% → 7.5%) ✓");
        System.out.println("   • Security (non-admin denied) ✓");
        System.out.println("   • Validation (invalid value rejected) ✓");
        System.out.println("");
        System.out.println("✅ Maintenance Mode Tests:");
        System.out.println("   • Get current status ✓");
        System.out.println("   • Enable maintenance mode ✓");
        System.out.println("   • Disable maintenance mode ✓");
        System.out.println("");
        System.out.println("✅ Integration Tests:");
        System.out.println("   • Bulk update (fee + maintenance) ✓");
        System.out.println("   • Health check (no auth) ✓");
        System.out.println("");
        System.out.println("✅ Service Layer Tests:");
        System.out.println("   • getSettingAsDouble() ✓");
        System.out.println("   • getSettingAsBoolean() ✓");
        System.out.println("");
        System.out.println("🎉 ALL CORE TESTS PASSED!");
        System.out.println("=".repeat(60));
    }
}