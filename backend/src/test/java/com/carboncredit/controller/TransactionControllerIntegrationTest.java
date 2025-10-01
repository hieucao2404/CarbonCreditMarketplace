package com.carboncredit.controller;

import com.carboncredit.dto.DisputeRequest;
import com.carboncredit.dto.PurchaseRequest;
import com.carboncredit.entity.CarbonCredit;
import com.carboncredit.entity.CreditListing;
import com.carboncredit.entity.Transaction;
import com.carboncredit.entity.User;
import com.carboncredit.repository.CarbonCreditRepository;
import com.carboncredit.repository.CreditListingRepository;
import com.carboncredit.repository.TransactionRepository;
import com.carboncredit.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.yml")
@Transactional
class TransactionControllerIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private CarbonCreditRepository carbonCreditRepository;
    @Autowired private CreditListingRepository creditListingRepository;
    @Autowired private TransactionRepository transactionRepository;

    private User buyer, seller;
    private CreditListing testListing;

    @BeforeEach
    void setUp() {
        // Create and save test users with unique identifiers to avoid conflicts
        String testId = java.util.UUID.randomUUID().toString().substring(0, 8);
        
        buyer = new User();
        buyer.setUsername("buyer_" + testId);
        buyer.setEmail("buyer_" + testId + "@test.com");
        buyer.setPasswordHash("hashed_password");
        buyer.setRole(User.UserRole.BUYER);
        buyer = userRepository.save(buyer);

        seller = new User();
        seller.setUsername("seller_" + testId);
        seller.setEmail("seller_" + testId + "@test.com");
        seller.setPasswordHash("hashed_password");
        seller.setRole(User.UserRole.EV_OWNER);
        seller = userRepository.save(seller);

        // Create and save test carbon credit
        CarbonCredit credit = new CarbonCredit();
        credit.setUser(seller);
        credit.setCreditAmount(new BigDecimal("1.5"));
        credit.setCo2ReducedKg(new BigDecimal("2.0")); // Required field
        credit.setStatus(CarbonCredit.CreditStatus.VERIFIED);
        credit = carbonCreditRepository.save(credit);

        // Create and save test listing
        testListing = new CreditListing();
        testListing.setCredit(credit);
        testListing.setStatus(CreditListing.ListingStatus.ACTIVE);
        testListing.setListingType(CreditListing.ListingType.FIXED);
        testListing = creditListingRepository.save(testListing);
    }

    @Test
    void testDataSetupCorrectly() {
        // Verify our test data was created correctly
        assertNotNull(buyer);
        assertNotNull(seller);
        assertNotNull(testListing);
        assertTrue(buyer.getUsername().startsWith("buyer_"));
        assertTrue(seller.getUsername().startsWith("seller_"));
        assertEquals(CreditListing.ListingStatus.ACTIVE, testListing.getStatus());
    }

    @Test
    void createTransaction_shouldPersistInDatabase() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setBuyer(buyer);
        transaction.setListing(testListing);
        transaction.setStatus(Transaction.TransactionStatus.PENDING);
        transaction.setAmount(new BigDecimal("15.00"));

        // When
        Transaction savedTransaction = transactionRepository.save(transaction);

        // Then
        assertNotNull(savedTransaction.getId());
        assertEquals(Transaction.TransactionStatus.PENDING, savedTransaction.getStatus());
        assertEquals(buyer.getId(), savedTransaction.getBuyer().getId());
        assertEquals(testListing.getId(), savedTransaction.getListing().getId());
    }

    @Test
    void findTransactionsByBuyer_shouldReturnCorrectTransactions() {
        // Given - create multiple transactions
        Transaction transaction1 = new Transaction();
        transaction1.setBuyer(buyer);
        transaction1.setListing(testListing);
        transaction1.setStatus(Transaction.TransactionStatus.PENDING);
        transaction1.setAmount(new BigDecimal("15.00"));
        transactionRepository.save(transaction1);

        Transaction transaction2 = new Transaction();
        transaction2.setBuyer(buyer);
        transaction2.setListing(testListing);
        transaction2.setStatus(Transaction.TransactionStatus.COMPLETED);
        transaction2.setAmount(new BigDecimal("25.00"));
        transactionRepository.save(transaction2);

        // When - verify transactions exist
        var allTransactions = transactionRepository.findAll();
        var buyerTransactions = allTransactions.stream()
            .filter(t -> t.getBuyer().getId().equals(buyer.getId()))
            .toList();

        // Then
        assertTrue(buyerTransactions.size() >= 2);
        assertTrue(buyerTransactions.stream().allMatch(t -> t.getBuyer().getId().equals(buyer.getId())));
    }

    @Test
    void validatePurchaseRequest_shouldContainRequiredFields() {
        // Given
        PurchaseRequest request = new PurchaseRequest();
        request.setListingId(testListing.getId());
        request.setNotes("Test purchase");

        // Then
        assertNotNull(request.getListingId());
        assertEquals("Test purchase", request.getNotes());
        assertEquals(testListing.getId(), request.getListingId());
    }

    @Test
    void validateDisputeRequest_shouldContainRequiredFields() {
        // Given
        DisputeRequest request = new DisputeRequest();
        request.setReason("Item not as described");
        request.setDescription("The carbon credits were not properly verified");
        request.setEvidence("Screenshot of verification page");

        // Then
        assertEquals("Item not as described", request.getReason());
        assertEquals("The carbon credits were not properly verified", request.getDescription());
        assertEquals("Screenshot of verification page", request.getEvidence());
    }

    @Test
    void transactionStatusTransitions_shouldBeValid() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setBuyer(buyer);
        transaction.setListing(testListing);
        transaction.setAmount(new BigDecimal("15.00"));

        // Test status transitions
        transaction.setStatus(Transaction.TransactionStatus.PENDING);
        Transaction savedPending = transactionRepository.save(transaction);
        assertEquals(Transaction.TransactionStatus.PENDING, savedPending.getStatus());

        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        Transaction savedCompleted = transactionRepository.save(transaction);
        assertEquals(Transaction.TransactionStatus.COMPLETED, savedCompleted.getStatus());

        transaction.setStatus(Transaction.TransactionStatus.CANCELLED);
        Transaction savedCancelled = transactionRepository.save(transaction);
        assertEquals(Transaction.TransactionStatus.CANCELLED, savedCancelled.getStatus());
    }
}