package com.carboncredit.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfiguration
@TestPropertySource(locations = "classpath:application-test.properties")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
public class ApiWorkflowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Order(1)
    public void testEVOwnerWorkflow() throws Exception {
        System.out.println("=== Testing EV Owner Workflow ===");
        
        // Step 1: Create EV Owner User
        String createUserJson = """
            {
                "username": "evowner1",
                "email": "evowner@test.com",
                "password": "password123",
                "role": "EV_OWNER",
                "fullName": "John EV Owner"
            }
            """;

        MvcResult userResult = mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createUserJson))
                .andDo(print())
                .andExpect(status().isCreated())
                .andReturn();

        System.out.println("✓ Step 1: User created successfully");

        // Step 2: Create Vehicle
        String createVehicleJson = """
            {
                "make": "Tesla",
                "model": "Model 3",
                "year": 2023,
                "batteryCapacity": 75.0,
                "efficiency": 0.15
            }
            """;

        mockMvc.perform(post("/api/vehicles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createVehicleJson))
                .andDo(print())
                .andExpect(status().isCreated());

        System.out.println("✓ Step 2: Vehicle created successfully");

        // Step 3: Create Journey
        String createJourneyJson = """
            {
                "startLocation": "Ho Chi Minh City",
                "endLocation": "Da Nang",
                "distance": 965.0,
                "energyConsumed": 144.75,
                "userId": 1,
                "vehicleId": 1
            }
            """;

        mockMvc.perform(post("/api/journeys")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJourneyJson))
                .andDo(print())
                .andExpect(status().isCreated());

        System.out.println("✓ Step 3: Journey created successfully");

        // Step 4: Generate Carbon Credits
        mockMvc.perform(post("/api/carbon-credits/generate")
                .param("journeyId", "1"))
                .andDo(print())
                .andExpect(status().isCreated());

        System.out.println("✓ Step 4: Carbon credits generated successfully");
    }

    @Test
    @Order(2)
    public void testCarbonCreditBuyerWorkflow() throws Exception {
        System.out.println("=== Testing Carbon Credit Buyer Workflow ===");
        
        // Step 1: Create Buyer User
        String createBuyerJson = """
            {
                "username": "buyer1",
                "email": "buyer@test.com",
                "password": "password123",
                "role": "CC_BUYER",
                "fullName": "Jane Buyer"
            }
            """;

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBuyerJson))
                .andDo(print())
                .andExpect(status().isCreated());

        System.out.println("✓ Step 1: Buyer created successfully");

        // Step 2: Search Available Credits
        mockMvc.perform(get("/api/credit-listings")
                .param("status", "ACTIVE"))
                .andDo(print())
                .andExpect(status().isOk());

        System.out.println("✓ Step 2: Credits searched successfully");

        // Step 3: Purchase Credits (assuming listing exists)
        String purchaseJson = """
            {
                "listingId": 1,
                "quantity": 10,
                "buyerId": 2
            }
            """;

        mockMvc.perform(post("/api/transactions/purchase")
                .contentType(MediaType.APPLICATION_JSON)
                .content(purchaseJson))
                .andDo(print())
                .andExpect(status().isCreated());

        System.out.println("✓ Step 3: Purchase transaction created successfully");
    }

    @Test
    @Order(3)
    public void testCVAWorkflow() throws Exception {
        System.out.println("=== Testing CVA Workflow ===");
        
        // Step 1: Create CVA User
        String createCVAJson = """
            {
                "username": "cva1",
                "email": "cva@test.com",
                "password": "password123",
                "role": "CVA",
                "fullName": "CVA Verifier"
            }
            """;

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createCVAJson))
                .andDo(print())
                .andExpect(status().isCreated());

        System.out.println("✓ Step 1: CVA user created successfully");

        // Step 2: Get Pending Credits for Verification
        mockMvc.perform(get("/api/carbon-credits")
                .param("status", "PENDING"))
                .andDo(print())
                .andExpect(status().isOk());

        System.out.println("✓ Step 2: Pending credits retrieved successfully");

        // Step 3: Verify Credit (assuming credit exists)
        mockMvc.perform(put("/api/carbon-credits/1/verify")
                .param("verifierId", "3")
                .param("approved", "true"))
                .andDo(print())
                .andExpect(status().isOk());

        System.out.println("✓ Step 3: Credit verified successfully");
    }

    @Test
    @Order(4)
    public void testAdminWorkflow() throws Exception {
        System.out.println("=== Testing Admin Workflow ===");
        
        // Step 1: Create Admin User
        String createAdminJson = """
            {
                "username": "admin1",
                "email": "admin@test.com",
                "password": "password123",
                "role": "ADMIN",
                "fullName": "System Admin"
            }
            """;

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createAdminJson))
                .andDo(print())
                .andExpect(status().isCreated());

        System.out.println("✓ Step 1: Admin user created successfully");

        // Step 2: Get All Users
        mockMvc.perform(get("/api/users"))
                .andDo(print())
                .andExpect(status().isOk());

        System.out.println("✓ Step 2: All users retrieved successfully");

        // Step 3: Get All Transactions
        mockMvc.perform(get("/api/transactions"))
                .andDo(print())
                .andExpect(status().isOk());

        System.out.println("✓ Step 3: All transactions retrieved successfully");
    }
}