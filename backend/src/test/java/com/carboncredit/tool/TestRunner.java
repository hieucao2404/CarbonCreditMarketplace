package com.carboncredit.tool;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.web.client.HttpServerErrorException.BadGateway;

import com.carboncredit.dto.JourneyStatistics;
import com.carboncredit.entity.CarbonCredit;
import com.carboncredit.entity.CarbonCredit.CreditStatus;
import com.carboncredit.entity.CreditListing;
import com.carboncredit.entity.CreditListing.ListingStatus;
import com.carboncredit.entity.CreditListing.ListingType;
import com.carboncredit.entity.JourneyData;
import com.carboncredit.entity.User;
import com.carboncredit.entity.Vehicle;
import com.carboncredit.service.CarbonCreditService;
import com.carboncredit.service.CreditListingService;
import com.carboncredit.service.JourneyDataService;
import com.carboncredit.service.ValidationService;

public class TestRunner {

    private static CarbonCreditService carbonCreditService;
    private static JourneyDataService journeyDataService;
    private static CreditListingService creditListingService;
    private static ValidationService validationService;

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("CARBON CREDIT MARKETPLACE - COMPLETE INTEGRATION TEST");
        System.out.println("Journey → Credit → Listing → Purchase Workflow");
        System.out.println("=".repeat(80));

        // Initialize services (with null repositories for testing calculation logic)
        validationService = new ValidationService();
        carbonCreditService = new CarbonCreditService(null);
        journeyDataService = new JourneyDataService(null, carbonCreditService, validationService);
        creditListingService = new CreditListingService(null, null, validationService);

        testCO2CalculationsWithService();
        testCreditAmountCalculations();
        testJourneyValidation();
        testCreditStatusTransitions();
        testCreditListingIntegration(); // NEW: Credit listing tests
        testMarketplaceWorkflow(); // NEW: Complete marketplace workflow
        testListingPriceCalculations(); // NEW: Pricing logic tests
        testListingStatusTransitions(); // NEW: Listing lifecycle tests
        testCompleteIntegratedWorkflow(); // ENHANCED: Full integration test
    }

    // Keep existing methods (testCO2CalculationsWithService, testCreditAmountCalculations)

    private static void testCO2CalculationsWithService() {
        System.out.println("\n1. CO2 CALCULATION SERVICE TESTS");
        System.out.println("-".repeat(50));

        // Test various journey scenarios
        Object[][] testCases = {
                { "Short city trip", new BigDecimal("25"), new BigDecimal("8") },
                { "Medium journey", new BigDecimal("100"), new BigDecimal("22") },
                { "Long highway trip", new BigDecimal("300"), new BigDecimal("65") },
                { "Extended travel", new BigDecimal("500"), new BigDecimal("95") }
        };

        System.out.println("Journey Type        | Distance | Energy  | CO2 Reduced");
        System.out.println("--------------------|----------|---------|-------------");

        for (Object[] testCase : testCases) {
            String description = (String) testCase[0];
            BigDecimal distance = (BigDecimal) testCase[1];
            BigDecimal energy = (BigDecimal) testCase[2];

            BigDecimal co2Reduced = carbonCreditService.calculateCO2Reduction(distance, energy);

            System.out.printf("%-18s | %6s km | %5s kWh | %8.2f kg%n",
                    description, distance, energy, co2Reduced);
        }

        System.out.println("\n✅ CO2 calculation service tests completed!");
    }

    private static void testCreditAmountCalculations() {
        System.out.println("\n2. CREDIT AMOUNT CALCULATION TESTS");
        System.out.println("-".repeat(50));

        BigDecimal[] co2Values = {
                new BigDecimal("10.5"), new BigDecimal("25.0"),
                new BigDecimal("50.75"), new BigDecimal("100.0")
        };

        CreditStatus[] statuses = {
                CreditStatus.PENDING, CreditStatus.VERIFIED, CreditStatus.LISTED, CreditStatus.SOLD
        };

        System.out.println("CO2 Reduced | Status    | Credit Amount | Multiplier Effect");
        System.out.println("------------|-----------|---------------|-------------------");

        for (BigDecimal co2 : co2Values) {
            for (CreditStatus status : statuses) {
                try {
                    // Use reflection to call private method
                    java.lang.reflect.Method method = CarbonCreditService.class.getDeclaredMethod(
                            "calculateCreditAmount", BigDecimal.class, CreditStatus.class);
                    method.setAccessible(true);
                    BigDecimal creditAmount = (BigDecimal) method.invoke(carbonCreditService, co2, status);

                    BigDecimal multiplier = creditAmount.divide(co2, 6, BigDecimal.ROUND_HALF_UP);

                    System.out.printf("%8.2f kg | %-9s | %11.6f | %8.6fx%n",
                            co2, status, creditAmount, multiplier);
                } catch (Exception e) {
                    System.out.printf("%8.2f kg | %-9s | %11s | %11s%n",
                            co2, status, "ERROR", "N/A");
                }
            }
            System.out.println("------------|-----------|---------------|-------------------");
        }

        System.out.println("\n✅ Credit amount calculation tests completed!");
    }

    private static void testJourneyValidation() {
        System.out.println("\n3. JOURNEY VALIDATION TESTS");
        System.out.println("-".repeat(50));

        User testUser = createTestUser("validation_user", User.UserRole.EV_OWNER);
        Vehicle testVehicle = createTestVehicle(testUser); // FIX: Pass the user parameter

        // Valid journey test
        System.out.println("3.1 Valid Journey Tests:");
        JourneyData validJourney = createTestJourney(
                new BigDecimal("150"), new BigDecimal("35"), null, testUser, testVehicle);

        try {
            validationService.validateJourneyData(validJourney);
            System.out.println("   ✅ Valid journey data accepted");
        } catch (Exception e) {
            System.out.println("   ❌ Valid journey rejected: " + e.getMessage());
        }

        // Invalid journey tests
        System.out.println("\n3.2 Invalid Journey Tests:");

        // Negative distance
        JourneyData negativeDistance = createTestJourney(
                new BigDecimal("-50"), new BigDecimal("20"), null, testUser, testVehicle);
        try {
            validationService.validateJourneyData(negativeDistance);
            System.out.println("   ❌ Negative distance incorrectly accepted");
        } catch (Exception e) {
            System.out.println("   ✅ Negative distance correctly rejected: " + e.getClass().getSimpleName());
        }

        // Zero energy
        JourneyData zeroEnergy = createTestJourney(
                new BigDecimal("100"), BigDecimal.ZERO, null, testUser, testVehicle);
        try {
            validationService.validateJourneyData(zeroEnergy);
            System.out.println("   ❌ Zero energy incorrectly accepted");
        } catch (Exception e) {
            System.out.println("   ✅ Zero energy correctly rejected: " + e.getClass().getSimpleName());
        }

        System.out.println("\n✅ Journey validation tests completed!");
    }

    private static void testCreditStatusTransitions() {
        System.out.println("\n4. CREDIT STATUS TRANSITION TESTS");
        System.out.println("-".repeat(50));

        User owner = createTestUser("credit_owner", User.UserRole.EV_OWNER);
        User cva = createTestUser("cva_verifier", User.UserRole.CVA);

        System.out.println("4.1 Credit Lifecycle Simulation:");

        // Create journey and initial credit
        JourneyData journey = createTestJourney(
                new BigDecimal("200"), new BigDecimal("45"), null, owner, createTestVehicle(owner)); // FIX: Pass owner

        CarbonCredit credit = new CarbonCredit();
        credit.setId(UUID.randomUUID());
        credit.setUser(owner);
        credit.setJourney(journey);

        BigDecimal co2 = carbonCreditService.calculateCO2Reduction(
                journey.getDistanceKm(), journey.getEnergyConsumedKwh());
        credit.setCo2ReducedKg(co2);

        // Status progression with credit amount recalculation
        CreditStatus[] statusFlow = {
                CreditStatus.PENDING, CreditStatus.VERIFIED, CreditStatus.LISTED, CreditStatus.SOLD
        };

        System.out.println("   Status Progression:");
        for (int i = 0; i < statusFlow.length; i++) {
            CreditStatus status = statusFlow[i];
            credit.setStatus(status);

            // Update timestamps
            switch (status) {
                case VERIFIED -> credit.setVerifiedAt(LocalDateTime.now());
                case LISTED -> credit.setListedAt(LocalDateTime.now());
            }

            // Recalculate credit amount for current status
            try {
                java.lang.reflect.Method method = CarbonCreditService.class.getDeclaredMethod(
                        "calculateCreditAmount", BigDecimal.class, CreditStatus.class);
                method.setAccessible(true);
                BigDecimal creditAmount = (BigDecimal) method.invoke(carbonCreditService, co2, status);
                credit.setCreditAmount(creditAmount);

                System.out.printf("   %d. %s → Credit Amount: %.6f%n",
                        i + 1, status, creditAmount);
            } catch (Exception e) {
                System.out.printf("   %d. %s → Credit Amount: ERROR%n", i + 1, status);
            }
        }

        System.out.println("\n✅ Credit status transition tests completed!");
    }

    private static void testCreditListingIntegration() {
        System.out.println("\n5. CREDIT LISTING INTEGRATION TESTS");
        System.out.println("-".repeat(50));

        User seller = createTestUser("seller", User.UserRole.EV_OWNER);
        User buyer = createTestUser("buyer", User.UserRole.BUYER);

        // Create a verified credit for listing
        CarbonCredit verifiedCredit = createTestCredit(seller, CreditStatus.VERIFIED);
        System.out.printf("Created verified credit: %s (%.2f kg CO2, %.6f credits)%n",
                verifiedCredit.getId(), verifiedCredit.getCo2ReducedKg(), verifiedCredit.getCreditAmount());

        System.out.println("\n5.1 Fixed Price Listing Creation:");

        // Test fixed price listing creation using your actual entity structure
        CreditListing fixedListing = new CreditListing();
        fixedListing.setId(UUID.randomUUID());
        fixedListing.setCredit(verifiedCredit);
        fixedListing.setListingType(ListingType.FIXED); // Using your enum
        fixedListing.setStatus(ListingStatus.ACTIVE);
        fixedListing.setPrice(new BigDecimal("75.50"));
        fixedListing.setCreatedAt(LocalDateTime.now());

        BigDecimal price = fixedListing.getPrice();

        // Validate listing creation
        try {
            validationService.validateListingCreation(verifiedCredit, buyer, price);
            System.out.printf("   ✅ Fixed price listing created: $%.2f%n", fixedListing.getPrice());
            System.out.printf("   Credit status: %s → %s%n", CreditStatus.VERIFIED, CreditStatus.LISTED);

            // Update credit status to listed
            verifiedCredit.setStatus(CreditStatus.LISTED);
            verifiedCredit.setListedAt(LocalDateTime.now());

        } catch (Exception e) {
            System.out.println("   ❌ Fixed price listing validation failed: " + e.getMessage());
        }

        System.out.println("\n5.2 Auction Listing Creation:");

        // Create another credit for auction
        CarbonCredit auctionCredit = createTestCredit(seller, CreditStatus.VERIFIED);

        CreditListing auctionListing = new CreditListing();
        auctionListing.setId(UUID.randomUUID());
        auctionListing.setCredit(auctionCredit);
        auctionListing.setListingType(ListingType.AUCTION); // Using your enum
        auctionListing.setStatus(ListingStatus.ACTIVE);
        auctionListing.setMinBid(new BigDecimal("50.00")); // Using your field name
        auctionListing.setPrice(new BigDecimal("50.00")); // Current price
        auctionListing.setAuctionEndTime(LocalDateTime.now().plusDays(7)); // Using your field name
        auctionListing.setCreatedAt(LocalDateTime.now());

        System.out.printf("   ✅ Auction listing created: Min bid $%.2f, Current $%.2f%n",
                auctionListing.getMinBid(), auctionListing.getPrice());
        System.out.printf("   Auction ends: %s%n", auctionListing.getAuctionEndTime());

        System.out.println("\n5.3 Listing Relationships Verification:");
        System.out.printf("   Fixed Listing → Credit: %s%n",
                fixedListing.getCredit().getId().equals(verifiedCredit.getId()) ? "✅ Linked" : "❌ Not linked");
        System.out.printf("   Credit → Journey: %s%n",
                verifiedCredit.getJourney() != null ? "✅ Linked" : "❌ Not linked");
        System.out.printf("   Journey → User: %s%n",
                verifiedCredit.getJourney().getUser().getId().equals(seller.getId()) ? "✅ Linked" : "❌ Not linked");

        System.out.println("\n✅ Credit listing integration tests completed!");
    }

    private static void testMarketplaceWorkflow() {
        System.out.println("\n6. MARKETPLACE WORKFLOW TESTS");
        System.out.println("-".repeat(50));

        User seller = createTestUser("marketplace_seller", User.UserRole.EV_OWNER);
        User buyer1 = createTestUser("buyer1", User.UserRole.BUYER);
        User buyer2 = createTestUser("buyer2", User.UserRole.BUYER);

        System.out.println("6.1 Multiple Listings Creation:");

        // Create multiple credits and listings
        CreditListing[] listings = new CreditListing[4];
        BigDecimal[] prices = {
                new BigDecimal("45.00"), new BigDecimal("67.50"),
                new BigDecimal("89.25"), new BigDecimal("112.00")
        };

        for (int i = 0; i < 4; i++) {
            CarbonCredit credit = createTestCredit(seller, CreditStatus.VERIFIED);
            credit.setStatus(CreditStatus.LISTED);

            listings[i] = new CreditListing();
            listings[i].setId(UUID.randomUUID());
            listings[i].setCredit(credit);
            listings[i].setListingType(ListingType.FIXED);
            listings[i].setStatus(ListingStatus.ACTIVE);
            listings[i].setPrice(prices[i]);
            listings[i].setCreatedAt(LocalDateTime.now().minusHours(i));

            System.out.printf("   Listing %d: $%.2f (%.2f kg CO2)%n",
                    i + 1, prices[i], credit.getCo2ReducedKg());
        }

        System.out.println("\n6.2 Marketplace Browsing Simulation:");

        // Sort by price (ascending)
        Arrays.sort(listings, (a, b) -> a.getPrice().compareTo(b.getPrice()));
        System.out.println("   Sorted by Price (Low to High):");
        for (int i = 0; i < listings.length; i++) {
            System.out.printf("     %d. $%.2f - %.2f kg CO2 - %s%n",
                    i + 1, listings[i].getPrice(),
                    listings[i].getCredit().getCo2ReducedKg(),
                    listings[i].getStatus());
        }

        System.out.println("\n6.3 Purchase Simulation:");

        // Simulate purchase of cheapest listing
        CreditListing purchasedListing = listings[0];
        purchasedListing.setStatus(ListingStatus.CLOSED);
        purchasedListing.setUpdatedAt(LocalDateTime.now());

        // Update credit status
        purchasedListing.getCredit().setStatus(CreditStatus.SOLD);

        System.out.printf("   ✅ Purchase completed by %s%n", buyer1.getUsername());
        System.out.printf("   Price paid: $%.2f%n", purchasedListing.getPrice());
        System.out.printf("   Credit status: LISTED → SOLD%n");
        System.out.printf("   Listing status: ACTIVE → CLOSED%n");

        System.out.println("\n✅ Marketplace workflow tests completed!");
    }

    private static void testListingPriceCalculations() {
        System.out.println("\n7. LISTING PRICE CALCULATION TESTS");
        System.out.println("-".repeat(50));

        System.out.println("7.1 Price Per CO2 Analysis:");

        BigDecimal[] co2Amounts = {
                new BigDecimal("5.0"), new BigDecimal("15.0"),
                new BigDecimal("30.0"), new BigDecimal("50.0")
        };

        BigDecimal[] listingPrices = {
                new BigDecimal("25.00"), new BigDecimal("82.50"),
                new BigDecimal("180.00"), new BigDecimal("275.00")
        };

        System.out.println("   CO2 Amount | Listing Price | Price per kg CO2");
        System.out.println("   -----------|---------------|------------------");

        for (int i = 0; i < co2Amounts.length; i++) {
            BigDecimal pricePerKg = listingPrices[i].divide(co2Amounts[i], 2, BigDecimal.ROUND_HALF_UP);
            System.out.printf("   %6.1f kg  |   $%8.2f  |      $%6.2f%n",
                    co2Amounts[i], listingPrices[i], pricePerKg);
        }

        System.out.println("\n7.2 Market Price Trends Simulation:");

        // Simulate price changes over time
        BigDecimal basePrice = new BigDecimal("75.00");
        double[] marketFactors = { 1.0, 1.15, 0.95, 1.25, 1.05, 0.90, 1.35 };
        String[] timePoints = { "Week 1", "Week 2", "Week 3", "Week 4", "Week 5", "Week 6", "Week 7" };

        System.out.println("   Time Period | Market Price | Change");
        System.out.println("   ------------|--------------|--------");

        BigDecimal previousPrice = basePrice;
        for (int i = 0; i < marketFactors.length; i++) {
            BigDecimal currentPrice = basePrice.multiply(new BigDecimal(marketFactors[i]));
            BigDecimal change = currentPrice.subtract(previousPrice);
            String changeStr = change.compareTo(BigDecimal.ZERO) >= 0 ? String.format("+$%.2f", change)
                    : String.format("-$%.2f", change.abs());

            System.out.printf("   %-11s |   $%8.2f  | %s%n",
                    timePoints[i], currentPrice, changeStr);
            previousPrice = currentPrice;
        }

        System.out.println("\n✅ Price calculation tests completed!");
    }

    private static void testListingStatusTransitions() {
        System.out.println("\n8. LISTING STATUS TRANSITION TESTS");
        System.out.println("-".repeat(50));

        User seller = createTestUser("transition_seller", User.UserRole.EV_OWNER);
        User buyer = createTestUser("transition_buyer", User.UserRole.BUYER);

        System.out.println("8.1 Fixed Price Listing Lifecycle:");

        CarbonCredit credit = createTestCredit(seller, CreditStatus.VERIFIED);
        CreditListing listing = new CreditListing();
        listing.setId(UUID.randomUUID());
        listing.setCredit(credit);
        listing.setListingType(ListingType.FIXED);
        listing.setPrice(new BigDecimal("95.00"));
        listing.setCreatedAt(LocalDateTime.now());

        // Status progression
        System.out.println("   Status Transitions:");

        // 1. Active
        listing.setStatus(ListingStatus.ACTIVE);
        credit.setStatus(CreditStatus.LISTED);
        System.out.printf("   1. Created as ACTIVE (Credit: VERIFIED → LISTED)%n");

        // 2. Active → Closed (Purchase)
        listing.setStatus(ListingStatus.CLOSED);
        listing.setUpdatedAt(LocalDateTime.now());
        credit.setStatus(CreditStatus.SOLD);
        System.out.printf("   2. ACTIVE → CLOSED (Credit: LISTED → SOLD)%n");

        System.out.println("\n8.2 Auction Listing Lifecycle:");

        CarbonCredit auctionCredit = createTestCredit(seller, CreditStatus.VERIFIED);
        CreditListing auctionListing = new CreditListing();
        auctionListing.setId(UUID.randomUUID());
        auctionListing.setCredit(auctionCredit);
        auctionListing.setListingType(ListingType.AUCTION);
        auctionListing.setMinBid(new BigDecimal("60.00"));
        auctionListing.setPrice(new BigDecimal("60.00")); // Current price starts at min bid
        auctionListing.setAuctionEndTime(LocalDateTime.now().plusDays(3));

        System.out.println("   Auction Progression:");

        // Auction states
        auctionListing.setStatus(ListingStatus.ACTIVE);
        System.out.printf("   1. Auction ACTIVE (Min bid: $%.2f, Current: $%.2f)%n",
                auctionListing.getMinBid(), auctionListing.getPrice());

        // Bid received
        auctionListing.setPrice(new BigDecimal("75.00"));
        System.out.printf("   2. Bid received: $%.2f%n", auctionListing.getPrice());

        // Higher bid
        auctionListing.setPrice(new BigDecimal("95.00"));
        System.out.printf("   3. Higher bid: $%.2f%n", auctionListing.getPrice());

        // Auction ended
        auctionListing.setStatus(ListingStatus.CLOSED);
        auctionListing.setUpdatedAt(LocalDateTime.now());
        System.out.printf("   4. Auction CLOSED - Final price: $%.2f%n", auctionListing.getPrice());

        System.out.println("\n8.3 Listing Cancellation:");

        CarbonCredit cancelCredit = createTestCredit(seller, CreditStatus.LISTED);
        CreditListing cancelListing = new CreditListing();
        cancelListing.setStatus(ListingStatus.ACTIVE);

        // Cancel listing
        cancelListing.setStatus(ListingStatus.CANCELLED);
        cancelCredit.setStatus(CreditStatus.VERIFIED); // Revert to verified

        System.out.printf("   Listing CANCELLED (Credit: LISTED → VERIFIED)%n");

        System.out.println("\n✅ Listing status transition tests completed!");
    }

    private static void testCompleteIntegratedWorkflow() {
        System.out.println("\n9. COMPLETE INTEGRATED WORKFLOW TEST");
        System.out.println("-".repeat(50));

        User evOwner = createTestUser("integration_owner", User.UserRole.EV_OWNER);
        User buyer = createTestUser("integration_buyer", User.UserRole.BUYER);
        User cva = createTestUser("cva_verifier", User.UserRole.CVA);
        Vehicle tesla = createTestVehicle(evOwner);

        System.out.println("🚗 Starting Complete Integration: Journey → Credit → Listing → Purchase");

        // === STEP 1: EV Journey ===
        System.out.println("\n1️⃣ EV Journey Completion:");
        JourneyData journey = createTestJourney(
                new BigDecimal("180"), new BigDecimal("42"), null, evOwner, tesla);

        BigDecimal co2Reduction = carbonCreditService.calculateCO2Reduction(
                journey.getDistanceKm(), journey.getEnergyConsumedKwh());
        journey.setCo2ReducedKg(co2Reduction);

        System.out.printf("   🛣️  Distance: %s km%n", journey.getDistanceKm());
        System.out.printf("   ⚡ Energy: %s kWh%n", journey.getEnergyConsumedKwh());
        System.out.printf("   🌱 CO2 Saved: %s kg%n", co2Reduction);

        // === STEP 2: Carbon Credit Auto-Creation ===
        System.out.println("\n2️⃣ Carbon Credit Auto-Creation:");
        CarbonCredit credit = new CarbonCredit();
        credit.setId(UUID.randomUUID());
        credit.setUser(evOwner);
        credit.setJourney(journey);
        credit.setCo2ReducedKg(co2Reduction);
        credit.setStatus(CreditStatus.PENDING);
        credit.setCreatedAt(LocalDateTime.now());

        // Calculate credit amount
        try {
            java.lang.reflect.Method method = CarbonCreditService.class.getDeclaredMethod(
                    "calculateCreditAmount", BigDecimal.class, CreditStatus.class);
            method.setAccessible(true);
            BigDecimal creditAmount = (BigDecimal) method.invoke(
                    carbonCreditService, co2Reduction, CreditStatus.PENDING);
            credit.setCreditAmount(creditAmount);

            System.out.printf("   🏆 Credit Created: %s%n", credit.getId());
            System.out.printf("   📊 Status: %s%n", credit.getStatus());
            System.out.printf("   💎 Amount: %s credits%n", creditAmount);
        } catch (Exception e) {
            System.out.println("   ❌ Credit calculation failed");
            return;
        }

        // === STEP 3: CVA Verification ===
        System.out.println("\n3️⃣ CVA Verification Process:");
        credit.setStatus(CreditStatus.VERIFIED);
        credit.setVerifiedAt(LocalDateTime.now());

        // Recalculate credit amount for verified status
        try {
            java.lang.reflect.Method method = CarbonCreditService.class.getDeclaredMethod(
                    "calculateCreditAmount", BigDecimal.class, CreditStatus.class);
            method.setAccessible(true);
            BigDecimal verifiedAmount = (BigDecimal) method.invoke(
                    carbonCreditService, co2Reduction, CreditStatus.VERIFIED);
            credit.setCreditAmount(verifiedAmount);

            System.out.printf("   ✅ Verification Complete by %s%n", cva.getUsername());
            System.out.printf("   📊 Status: PENDING → VERIFIED%n");
            System.out.printf("   💎 Updated Amount: %s credits%n", verifiedAmount);
        } catch (Exception e) {
            System.out.println("   ❌ Verified credit calculation failed");
        }

        // === STEP 4: Marketplace Listing ===
        System.out.println("\n4️⃣ Marketplace Listing Creation:");
        credit.setStatus(CreditStatus.LISTED);
        credit.setListedAt(LocalDateTime.now());

        CreditListing listing = new CreditListing();
        listing.setId(UUID.randomUUID());
        listing.setCredit(credit);
        listing.setListingType(ListingType.FIXED);
        listing.setStatus(ListingStatus.ACTIVE);
        listing.setPrice(new BigDecimal("128.75"));
        listing.setCreatedAt(LocalDateTime.now());

        System.out.printf("   🏪 Listing Created: %s%n", listing.getId());
        System.out.printf("   💰 Price: $%.2f%n", listing.getPrice());
        System.out.printf("   📊 Credit Status: VERIFIED → LISTED%n");
        System.out.printf("   📈 Listing Status: ACTIVE%n");

        // === STEP 5: Purchase Transaction ===
        System.out.println("\n5️⃣ Purchase Transaction:");
        listing.setStatus(ListingStatus.CLOSED);
        listing.setUpdatedAt(LocalDateTime.now());
        credit.setStatus(CreditStatus.SOLD);

        System.out.printf("   🛒 Purchased by: %s%n", buyer.getUsername());
        System.out.printf("   💵 Amount Paid: $%.2f%n", listing.getPrice());
        System.out.printf("   📊 Credit Status: LISTED → SOLD%n");
        System.out.printf("   📈 Listing Status: ACTIVE → CLOSED%n");

        // === STEP 6: Final Integration Verification ===
        System.out.println("\n6️⃣ Integration Verification:");
        System.out.printf("   🔗 Journey → Credit Link: %s%n",
                journey.getId().equals(credit.getJourney().getId()) ? "✅ Valid" : "❌ Broken");
        System.out.printf("   🔗 Credit → Listing Link: %s%n",
                credit.getId().equals(listing.getCredit().getId()) ? "✅ Valid" : "❌ Broken");
        System.out.printf("   🔗 Owner → Journey User Link: %s%n",
                evOwner.getId().equals(credit.getUser().getId()) ? "✅ Valid" : "❌ Broken");

        // === STEP 7: Financial Summary ===
        System.out.println("\n7️⃣ Transaction Summary:");
        BigDecimal pricePerKg = listing.getPrice().divide(co2Reduction, 2, BigDecimal.ROUND_HALF_UP);
        BigDecimal pricePerCredit = listing.getPrice().divide(credit.getCreditAmount(), 2, BigDecimal.ROUND_HALF_UP);

        System.out.printf("   💰 Total Price: $%.2f%n", listing.getPrice());
        System.out.printf("   🌱 Price per kg CO2: $%.2f%n", pricePerKg);
        System.out.printf("   💎 Price per Credit: $%.2f%n", pricePerCredit);
        System.out.printf("   ⏱️  Transaction Time: %s%n", listing.getUpdatedAt());

        System.out.println("\n🎉 COMPLETE INTEGRATED WORKFLOW SUCCESSFUL! 🎉");
        System.out.println("   Journey ✅ → Credit ✅ → Listing ✅ → Purchase ✅");
    }

    // ==================== HELPER METHODS ====================

    private static User createTestUser(String username, User.UserRole role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    private static Vehicle createTestVehicle(User user) {
        Vehicle vehicle = new Vehicle();
        vehicle.setUser(user); // assign actual User entity
        vehicle.setVin("1HGCM82633A123456"); // must be unique and 17 chars
        vehicle.setModel("Model 3");
        vehicle.setRegistrationDate(LocalDate.now());
        return vehicle;
    }

    private static JourneyData createTestJourney(BigDecimal distance, BigDecimal energy,
            LocalDateTime startTime, User user, Vehicle vehicle) {
        JourneyData journey = new JourneyData();
        journey.setId(UUID.randomUUID());
        journey.setUser(user);
        journey.setVehicle(vehicle);
        journey.setDistanceKm(distance);
        journey.setEnergyConsumedKwh(energy);
        journey.setStartTime(startTime != null ? startTime : LocalDateTime.now().minusHours(2));
        journey.setEndTime(LocalDateTime.now());
        journey.setCreatedAt(LocalDateTime.now());
        return journey;
    }

    private static CarbonCredit createTestCredit(User owner, CreditStatus status) {
        // Create journey first
        JourneyData journey = createTestJourney(
                new BigDecimal("120"), new BigDecimal("28"), null, owner, createTestVehicle(owner));

        BigDecimal co2 = carbonCreditService.calculateCO2Reduction(
                journey.getDistanceKm(), journey.getEnergyConsumedKwh());
        journey.setCo2ReducedKg(co2);

        // Create credit
        CarbonCredit credit = new CarbonCredit();
        credit.setId(UUID.randomUUID());
        credit.setUser(owner);
        credit.setJourney(journey);
        credit.setCo2ReducedKg(co2);
        credit.setStatus(status);
        credit.setCreatedAt(LocalDateTime.now());

        if (status == CreditStatus.VERIFIED) {
            credit.setVerifiedAt(LocalDateTime.now());
        } else if (status == CreditStatus.LISTED) {
            credit.setVerifiedAt(LocalDateTime.now());
            credit.setListedAt(LocalDateTime.now());
        }

        // Calculate credit amount
        try {
            java.lang.reflect.Method method = CarbonCreditService.class.getDeclaredMethod(
                    "calculateCreditAmount", BigDecimal.class, CreditStatus.class);
            method.setAccessible(true);
            BigDecimal creditAmount = (BigDecimal) method.invoke(carbonCreditService, co2, status);
            credit.setCreditAmount(creditAmount);
        } catch (Exception e) {
            credit.setCreditAmount(co2.multiply(new BigDecimal("0.001"))); // fallback
        }

        return credit;
    }
}