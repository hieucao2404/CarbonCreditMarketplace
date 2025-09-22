package com.carboncredit.service;

import com.carboncredit.entity.CarbonCredit;
import com.carboncredit.entity.CarbonCredit.CreditStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class CarbonCreditServiceTest {

    @Test
    @DisplayName("Test CO2 Reduction Calculation")
    public void testCO2ReductionCalculation() {
        CarbonCreditService service = new CarbonCreditService(null, null);
        
        // Test case: 100km trip, 25kWh consumption
        BigDecimal distance = new BigDecimal("100");
        BigDecimal energy = new BigDecimal("25");
        
        BigDecimal reduction = service.calculateCO2Reduction(distance, energy);
        
        // Expected: (100 * 0.21) - (25 * 0.5) = 21 - 12.5 = 8.5 kg CO2
        assertEquals(0, new BigDecimal("8.50").compareTo(reduction));
        
        System.out.println("CO2 Reduction for 100km trip with 25kWh: " + reduction + " kg");
    }

    @Test
    @DisplayName("Test Status-Based Credit Amount Calculation")
    public void testStatusBasedCreditCalculation() throws Exception {
        CarbonCreditService service = new CarbonCreditService(null, null);
        
        // Use reflection to access private method
        Method method = CarbonCreditService.class.getDeclaredMethod("calculateCreditAmount", BigDecimal.class, CreditStatus.class);
        method.setAccessible(true);
        
        BigDecimal co2Amount = new BigDecimal("25.0"); // 25kg CO2 reduction
        
        // Test PENDING status (70% of base calculation)
        BigDecimal pendingCredits = (BigDecimal) method.invoke(service, co2Amount, CreditStatus.PENDING);
        
        // Test VERIFIED status (100% of base calculation)
        BigDecimal verifiedCredits = (BigDecimal) method.invoke(service, co2Amount, CreditStatus.VERIFIED);
        
        // Test REJECTED status (should be zero)
        BigDecimal rejectedCredits = (BigDecimal) method.invoke(service, co2Amount, CreditStatus.REJECTED);
        
        System.out.println("\n=== Status-Based Credit Calculations for 25kg CO2 ===");
        System.out.println("PENDING credits: " + pendingCredits + " credits (70% rate)");
        System.out.println("VERIFIED credits: " + verifiedCredits + " credits (100% rate)");
        System.out.println("REJECTED credits: " + rejectedCredits + " credits");
        
        // Assertions
        assertTrue(verifiedCredits.compareTo(pendingCredits) > 0, "Verified credits should be higher than pending");
        assertEquals(0, BigDecimal.ZERO.compareTo(rejectedCredits), "Rejected credits should be zero");
        
        // Test long trip bonus (50kg CO2 should get 1.5x multiplier)
        BigDecimal longTripCredits = (BigDecimal) method.invoke(service, new BigDecimal("50.0"), CreditStatus.VERIFIED);
        System.out.println("\nLong trip (50kg CO2) VERIFIED credits: " + longTripCredits + " credits (1.5x bonus)");
        
        // Test short trip penalty (<5kg CO2 should get 0.5x multiplier)
        BigDecimal shortTripCredits = (BigDecimal) method.invoke(service, new BigDecimal("3.0"), CreditStatus.VERIFIED);
        System.out.println("Short trip (3kg CO2) VERIFIED credits: " + shortTripCredits + " credits (0.5x penalty)");
    }

    @Test
    @DisplayName("Test Complete Journey to Credit Workflow")
    public void testJourneyToCreditWorkflow() throws Exception {
        CarbonCreditService service = new CarbonCreditService(null, null);
        
        Method method = CarbonCreditService.class.getDeclaredMethod("calculateCreditAmount", BigDecimal.class, CreditStatus.class);
        method.setAccessible(true);
        
        // Simulate a real journey
        BigDecimal distance = new BigDecimal("150"); // 150km trip
        BigDecimal energy = new BigDecimal("35"); // 35kWh consumption
        
        // Step 1: Calculate CO2 reduction
        BigDecimal co2Reduction = service.calculateCO2Reduction(distance, energy);
        System.out.println("\n=== Complete Journey Workflow ===");
        System.out.println("Journey: " + distance + "km, " + energy + "kWh");
        System.out.println("CO2 Reduction: " + co2Reduction + " kg");
        
        // Step 2: Calculate credits for each status
        BigDecimal pendingCredits = (BigDecimal) method.invoke(service, co2Reduction, CreditStatus.PENDING);
        BigDecimal verifiedCredits = (BigDecimal) method.invoke(service, co2Reduction, CreditStatus.VERIFIED);
        BigDecimal listedCredits = (BigDecimal) method.invoke(service, co2Reduction, CreditStatus.LISTED);
        
        System.out.println("\nCredit Evolution:");
        System.out.println("1. PENDING: " + pendingCredits + " credits");
        System.out.println("2. VERIFIED: " + verifiedCredits + " credits");
        System.out.println("3. LISTED: " + listedCredits + " credits");
        
        // Value increase from verification
        BigDecimal valueIncrease = verifiedCredits.subtract(pendingCredits);
        System.out.println("\nValue increase after verification: +" + valueIncrease + " credits");
    }
}
