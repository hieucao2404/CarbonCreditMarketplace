package com.carboncredit.controller;

import com.carboncredit.dto.DisputeRequest;
import com.carboncredit.dto.PurchaseRequest;
import com.carboncredit.entity.CarbonCredit;
import com.carboncredit.entity.CreditListing;
import com.carboncredit.entity.Dispute;
import com.carboncredit.entity.Transaction;
import com.carboncredit.entity.User;
import com.carboncredit.service.TransactionService;
import com.carboncredit.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransactionControllerTest {

    @Mock private TransactionService transactionService;
    @Mock private UserService userService;
    @Mock private Authentication authentication;

    private TransactionController transactionController;
    private User buyer, seller, admin;
    private Transaction testTransaction;
    private CreditListing testListing;
    private PurchaseRequest purchaseRequest;
    private DisputeRequest disputeRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        transactionController = new TransactionController(transactionService, userService);

        // Create test users
        buyer = new User();
        buyer.setId(UUID.randomUUID());
        buyer.setUsername("buyer");
        buyer.setRole(User.UserRole.BUYER);

        seller = new User();
        seller.setId(UUID.randomUUID());
        seller.setUsername("seller");
        seller.setRole(User.UserRole.EV_OWNER);

        admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setUsername("admin");
        admin.setRole(User.UserRole.ADMIN);

        // Create test listing and transaction
        CarbonCredit credit = new CarbonCredit();
        credit.setId(UUID.randomUUID());
        credit.setUser(seller);
        credit.setCreditAmount(new BigDecimal("1.5"));

        testListing = new CreditListing();
        testListing.setId(UUID.randomUUID());
        testListing.setCredit(credit);

        testTransaction = new Transaction();
        testTransaction.setId(UUID.randomUUID());
        testTransaction.setBuyer(buyer);
        testTransaction.setListing(testListing);
        testTransaction.setStatus(Transaction.TransactionStatus.PENDING);
        testTransaction.setAmount(new BigDecimal("15.00"));
        testTransaction.setCreatedAt(LocalDateTime.now());

        // Create test requests
        purchaseRequest = new PurchaseRequest();
        purchaseRequest.setListingId(testListing.getId());
        purchaseRequest.setNotes("Test purchase");

        disputeRequest = new DisputeRequest();
        disputeRequest.setReason("Item not as described");
    }

    @Test
    void initiateTransaction_shouldReturnCreated_whenValidRequest() {
        // Given
        when(authentication.getName()).thenReturn("buyer");
        when(userService.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(transactionService.initiatePurchase(testListing.getId(), buyer)).thenReturn(testTransaction);

        // When
        ResponseEntity<Transaction> response = transactionController.initiateTransaction(purchaseRequest, authentication);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Transaction responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(testTransaction.getId(), responseBody.getId());
        verify(transactionService).initiatePurchase(testListing.getId(), buyer);
    }

    @Test
    void initiateTransaction_shouldReturnBadRequest_whenUserNotFound() {
        // Given
        when(authentication.getName()).thenReturn("nonexistent");
        when(userService.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When
        ResponseEntity<Transaction> response = transactionController.initiateTransaction(purchaseRequest, authentication);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(transactionService, never()).initiatePurchase(any(), any());
    }

    @Test
    void completeTransaction_shouldReturnOk_whenBuyerAuthorized() {
        // Given
        UUID transactionId = testTransaction.getId();
        when(authentication.getName()).thenReturn("buyer");
        when(userService.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(transactionService.findTransactionById(transactionId)).thenReturn(testTransaction);
        when(transactionService.completeTransaction(testTransaction)).thenReturn(testTransaction);

        // When
        ResponseEntity<Transaction> response = transactionController.completeTransaction(transactionId, authentication);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(transactionService).processPayment(testTransaction);
        verify(transactionService).completeTransaction(testTransaction);
    }

    @Test
    void completeTransaction_shouldReturnForbidden_whenUnauthorized() {
        // Given
        UUID transactionId = testTransaction.getId();
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        otherUser.setUsername("other");

        when(authentication.getName()).thenReturn("other");
        when(userService.findByUsername("other")).thenReturn(Optional.of(otherUser));
        when(transactionService.findTransactionById(transactionId)).thenReturn(testTransaction);

        // When
        ResponseEntity<Transaction> response = transactionController.completeTransaction(transactionId, authentication);

        // Then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(transactionService, never()).processPayment(any());
    }

    @Test
    void completeTransaction_shouldReturnNotFound_whenTransactionNotExists() {
        // Given
        UUID transactionId = UUID.randomUUID();
        when(authentication.getName()).thenReturn("buyer");
        when(userService.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(transactionService.findTransactionById(transactionId)).thenReturn(null);

        // When
        ResponseEntity<Transaction> response = transactionController.completeTransaction(transactionId, authentication);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void cancelTransaction_shouldReturnOk_whenValidRequest() {
        // Given
        UUID transactionId = testTransaction.getId();
        when(authentication.getName()).thenReturn("buyer");
        when(userService.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(transactionService.cancelTransaction(transactionId, buyer)).thenReturn(testTransaction);

        // When
        ResponseEntity<Transaction> response = transactionController.cancelTransaction(transactionId, authentication);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(transactionService).cancelTransaction(transactionId, buyer);
    }

    @Test
    void getTransaction_shouldReturnOk_whenAuthorizedUser() {
        // Given
        UUID transactionId = testTransaction.getId();
        when(authentication.getName()).thenReturn("buyer");
        when(userService.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(transactionService.findTransactionById(transactionId)).thenReturn(testTransaction);

        // When
        ResponseEntity<Transaction> response = transactionController.getTransaction(transactionId, authentication);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void getTransaction_shouldReturnForbidden_whenUnauthorized() {
        // Given
        UUID transactionId = testTransaction.getId();
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        otherUser.setUsername("other");
        otherUser.setRole(User.UserRole.BUYER);

        when(authentication.getName()).thenReturn("other");
        when(userService.findByUsername("other")).thenReturn(Optional.of(otherUser));
        when(transactionService.findTransactionById(transactionId)).thenReturn(testTransaction);

        // When
        ResponseEntity<Transaction> response = transactionController.getTransaction(transactionId, authentication);

        // Then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void getMyTransactions_shouldReturnTransactions_whenValidUser() {
        // Given
        Page<Transaction> transactionsPage = new PageImpl<>(List.of(testTransaction));
        when(authentication.getName()).thenReturn("buyer");
        when(userService.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(transactionService.getUserTransactions(buyer, 0, 10)).thenReturn(transactionsPage);

        // When
        ResponseEntity<Page<Transaction>> response = transactionController.getMyTransactions(0, 10, authentication);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Page<Transaction> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(1, responseBody.getTotalElements());
    }

    @Test
    void getPurchaseHistory_shouldReturnPurchases_whenValidBuyer() {
        // Given
        Page<Transaction> purchasesPage = new PageImpl<>(List.of(testTransaction));
        when(authentication.getName()).thenReturn("buyer");
        when(userService.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(transactionService.getPurchaseHistory(buyer, 0, 10)).thenReturn(purchasesPage);

        // When
        ResponseEntity<Page<Transaction>> response = transactionController.getPurchaseHistory(0, 10, authentication);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Page<Transaction> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(1, responseBody.getTotalElements());
    }

    @Test
    void getSalesHistory_shouldReturnSales_whenValidSeller() {
        // Given
        Page<Transaction> salesPage = new PageImpl<>(List.of(testTransaction));
        when(authentication.getName()).thenReturn("seller");
        when(userService.findByUsername("seller")).thenReturn(Optional.of(seller));
        when(transactionService.getSalesHistory(seller, 0, 10)).thenReturn(salesPage);

        // When
        ResponseEntity<Page<Transaction>> response = transactionController.getSalesHistory(0, 10, authentication);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Page<Transaction> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(1, responseBody.getTotalElements());
    }

    @Test
    void createDispute_shouldReturnCreated_whenValidRequest() {
        // Given
        UUID transactionId = testTransaction.getId();
        Dispute dispute = new Dispute();
        dispute.setId(UUID.randomUUID());
        dispute.setReason("Item not as described");

        when(authentication.getName()).thenReturn("buyer");
        when(userService.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(transactionService.createDispute(transactionId, buyer, "Item not as described")).thenReturn(dispute);

        // When
        ResponseEntity<Dispute> response = transactionController.createDispute(transactionId, disputeRequest, authentication);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Dispute responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(dispute.getId(), responseBody.getId());
        verify(transactionService).createDispute(transactionId, buyer, "Item not as described");
    }

    @Test
    void getDisputedTransactions_shouldReturnTransactions_whenAdmin() {
        // Given
        Page<Transaction> disputedPage = new PageImpl<>(List.of(testTransaction));
        when(authentication.getName()).thenReturn("admin");
        when(userService.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(transactionService.getDisputedTransactions(0, 10)).thenReturn(disputedPage);

        // When
        ResponseEntity<Page<Transaction>> response = transactionController.getDisputedTransactions(0, 10, authentication);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Page<Transaction> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(1, responseBody.getTotalElements());
    }

    @Test
    void getDisputedTransactions_shouldReturnForbidden_whenNotAdmin() {
        // Given
        when(authentication.getName()).thenReturn("buyer");
        when(userService.findByUsername("buyer")).thenReturn(Optional.of(buyer));

        // When
        ResponseEntity<Page<Transaction>> response = transactionController.getDisputedTransactions(0, 10, authentication);

        // Then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(transactionService, never()).getDisputedTransactions(anyInt(), anyInt());
    }

    @Test
    void getTransactionStatistics_shouldReturnStats_whenAdmin() {
        // Given
        Map<String, Object> stats = Map.of(
            "totalTransactions", 100,
            "totalAmount", new BigDecimal("1500.00"),
            "completedTransactions", 85
        );
        when(authentication.getName()).thenReturn("admin");
        when(userService.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(transactionService.getTransactionStatistics(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(stats);

        // When
        ResponseEntity<Map<String, Object>> response = transactionController.getTransactionStatistics(null, null, authentication);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(100, responseBody.get("totalTransactions"));
    }

    @Test
    void getTransactionStatistics_shouldReturnForbidden_whenNotAdmin() {
        // Given
        when(authentication.getName()).thenReturn("buyer");
        when(userService.findByUsername("buyer")).thenReturn(Optional.of(buyer));

        // When
        ResponseEntity<Map<String, Object>> response = transactionController.getTransactionStatistics(null, null, authentication);

        // Then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(transactionService, never()).getTransactionStatistics(any(), any());
    }
}