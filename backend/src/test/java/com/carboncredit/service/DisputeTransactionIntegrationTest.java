// package com.carboncredit.service;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyString;
// import static org.mockito.ArgumentMatchers.eq;
// import static org.mockito.Mockito.*;
// import static org.mockito.Mockito.lenient;

// import java.math.BigDecimal;
// import java.time.LocalDateTime;
// import java.util.Arrays;
// import java.util.List;
// import java.util.Optional;
// import java.util.UUID;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.data.domain.Page;
// import org.springframework.data.domain.PageImpl;
// import org.springframework.data.domain.Pageable;

// import com.carboncredit.entity.CarbonCredit;
// import com.carboncredit.entity.CreditListing;
// import com.carboncredit.entity.Dispute;
// import com.carboncredit.entity.Transaction;
// import com.carboncredit.entity.User;
// import com.carboncredit.exception.EntityNotFoundException;
// import com.carboncredit.exception.BusinessOperationException;
// import com.carboncredit.repository.DisputeRepository;
// import com.carboncredit.repository.TransactionRepository;
// import com.carboncredit.repository.CreditListingRepository;
// import com.carboncredit.repository.CarbonCreditRepository;

// /**
//  * Comprehensive integration test for Dispute and Transaction services
//  * Tests the interaction between DisputeService and TransactionService
//  */
// @ExtendWith(MockitoExtension.class)
// @DisplayName("Dispute and Transaction Service Integration Tests")
// class DisputeTransactionIntegrationTest {

//     // === REPOSITORIES ===
//     @Mock private DisputeRepository disputeRepository;
//     @Mock private TransactionRepository transactionRepository;
//     @Mock private CreditListingRepository creditListingRepository;
//     @Mock private CarbonCreditRepository carbonCreditRepository;
    
//     // === SERVICES ===
//     @Mock private ValidationService validationService;
//     @Mock private PaymentService paymentService;
//     @Mock private NotificationService notificationService;
//     @Mock private AuditService auditService;
    
//     // === SERVICES UNDER TEST ===
//     @InjectMocks private DisputeService disputeService;
//     @InjectMocks private TransactionService transactionService;

//     // === TEST DATA ===
//     private User buyer;
//     private User seller;
//     private User admin;
//     private CarbonCredit credit;
//     private CreditListing listing;
//     private Transaction transaction;
//     private Dispute dispute;

//     @BeforeEach
//     void setUp() {
//         // Create test users with correct enum values
//         buyer = createUser("buyer@test.com", "Buyer User", User.UserRole.BUYER);
//         seller = createUser("seller@test.com", "Seller User", User.UserRole.EV_OWNER);
//         admin = createUser("admin@test.com", "Admin User", User.UserRole.ADMIN);
        
//         // Create test entities
//         credit = createCarbonCredit();
//         listing = createCreditListing();
//         transaction = createTransaction();
//         dispute = createDispute();
        
//         // Set up circular references needed for the services to work together
//         disputeService.transactionService = transactionService;
        
//         // Mock common validation calls to avoid NullPointerExceptions - using lenient to avoid UnnecessaryStubbingException
//         lenient().doNothing().when(validationService).validateId(any(UUID.class), anyString());
//         lenient().doNothing().when(validationService).validateUser(any(User.class));
//         lenient().doNothing().when(validationService).validatePageParameters(anyInt(), anyInt());
//     }

//     // ============== CORE DISPUTE OPERATIONS TESTS ==============

//     @Test
//     @DisplayName("Should find dispute by ID successfully")
//     void testFindDisputeById() {
//         // Given
//         UUID disputeId = dispute.getId();
//         when(disputeRepository.findById(disputeId)).thenReturn(Optional.of(dispute));

//         // When
//         Dispute result = disputeService.findDisputeById(disputeId);

//         // Then
//         assertNotNull(result);
//         assertEquals(disputeId, result.getId());
//         assertEquals("Payment not received", result.getReason());
//         verify(disputeRepository).findById(disputeId);
//         verify(validationService).validateId(disputeId, "Dispute");
//     }

//     @Test
//     @DisplayName("Should throw exception when dispute not found")
//     void testFindDisputeByIdNotFound() {
//         // Given
//         UUID nonExistentId = UUID.randomUUID();
//         when(disputeRepository.findById(nonExistentId)).thenReturn(Optional.empty());

//         // When & Then
//         EntityNotFoundException exception = assertThrows(
//             EntityNotFoundException.class,
//             () -> disputeService.findDisputeById(nonExistentId)
//         );
        
//         assertTrue(exception.getMessage().contains("Dispute not found with ID: " + nonExistentId));
//         verify(disputeRepository).findById(nonExistentId);
//     }

//     @Test
//     @DisplayName("Should get open disputes successfully")
//     void testGetOpenDisputes() {
//         // Given
//         List<Dispute> openDisputes = Arrays.asList(dispute);
//         when(disputeRepository.findOpenDisputesOrderByCreatedAt()).thenReturn(openDisputes);

//         // When
//         List<Dispute> result = disputeService.getOpenDisputes();

//         // Then
//         assertNotNull(result);
//         assertEquals(1, result.size());
//         assertEquals(dispute.getId(), result.get(0).getId());
//         verify(disputeRepository).findOpenDisputesOrderByCreatedAt();
//     }

//     @Test
//     @DisplayName("Should get user disputes successfully")
//     void testGetUserDisputes() {
//         // Given
//         List<Dispute> disputeList = Arrays.asList(dispute);
//         Page<Dispute> disputePage = new PageImpl<>(disputeList);
        
//         when(disputeRepository.findByUserInvolvedOrderByCreatedAtDesc(eq(buyer.getId()), any(Pageable.class)))
//             .thenReturn(disputePage);

//         // When
//         Page<Dispute> result = disputeService.getUserDisputes(buyer, 0, 10);

//         // Then
//         assertNotNull(result);
//         assertEquals(1, result.getTotalElements());
//         verify(disputeRepository).findByUserInvolvedOrderByCreatedAtDesc(eq(buyer.getId()), any(Pageable.class));
//         verify(validationService).validateUser(buyer);
//     }

//     @Test
//     @DisplayName("Should check if user has open disputes")
//     void testHasOpenDisputes() {
//         // Given
//         when(disputeRepository.hasOpenDisputes(buyer.getId())).thenReturn(true);

//         // When
//         boolean hasOpenDisputes = disputeService.hasOpenDisputes(buyer);

//         // Then
//         assertTrue(hasOpenDisputes);
//         verify(disputeRepository).hasOpenDisputes(buyer.getId());
//         verify(validationService).validateUser(buyer);
//     }

//     @Test
//     @DisplayName("Should get user dispute count")
//     void testGetUserDisputeCount() {
//         // Given
//         when(disputeRepository.countUserDisputes(buyer.getId())).thenReturn(5L);

//         // When
//         long disputeCount = disputeService.getUserDisputeCount(buyer);

//         // Then
//         assertEquals(5L, disputeCount);
//         verify(disputeRepository).countUserDisputes(buyer.getId());
//         verify(validationService).validateUser(buyer);
//     }

//     @Test
//     @DisplayName("Should get disputes for transaction")
//     void testGetDisputesForTransaction() {
//         // Given
//         UUID transactionId = transaction.getId();
//         List<Dispute> disputes = Arrays.asList(dispute);
//         when(disputeRepository.findByTransactionId(transactionId)).thenReturn(disputes);

//         // When
//         List<Dispute> result = disputeService.getDisputesForTransaction(transactionId);

//         // Then
//         assertNotNull(result);
//         assertEquals(1, result.size());
//         assertEquals(dispute.getId(), result.get(0).getId());
//         verify(disputeRepository).findByTransactionId(transactionId);
//         verify(validationService).validateId(transactionId, "Transaction");
//     }

//     @Test
//     @DisplayName("Should get latest dispute for transaction")
//     void testGetLatestDisputeForTransaction() {
//         // Given
//         UUID transactionId = transaction.getId();
//         when(disputeRepository.findLatestDisputeForTransaction(transactionId))
//             .thenReturn(Optional.of(dispute));

//         // When
//         Optional<Dispute> result = disputeService.getLatestDisputeForTransaction(transactionId);

//         // Then
//         assertTrue(result.isPresent());
//         assertEquals(dispute.getId(), result.get().getId());
//         verify(disputeRepository).findLatestDisputeForTransaction(transactionId);
//         verify(validationService).validateId(transactionId, "Transaction");
//     }

//     // ============== TRANSACTION OPERATIONS TESTS ==============

//     @Test
//     @DisplayName("Should find transaction by ID successfully")
//     void testFindTransactionById() {
//         // Given
//         UUID transactionId = transaction.getId();
//         when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

//         // When
//         Transaction result = transactionService.findTransactionById(transactionId);

//         // Then
//         assertNotNull(result);
//         assertEquals(transactionId, result.getId());
//         assertEquals(Transaction.TransactionStatus.COMPLETED, result.getStatus());
//         verify(transactionRepository).findById(transactionId);
//         verify(validationService).validateId(transactionId, "Transaction");
//     }

//     @Test
//     @DisplayName("Should throw exception when transaction not found")
//     void testTransactionNotFound() {
//         // Given
//         UUID nonExistentId = UUID.randomUUID();
//         when(transactionRepository.findById(nonExistentId)).thenReturn(Optional.empty());

//         // When & Then
//         EntityNotFoundException exception = assertThrows(
//             EntityNotFoundException.class,
//             () -> transactionService.findTransactionById(nonExistentId)
//         );
        
//         assertTrue(exception.getMessage().contains("Transaction not found with ID: " + nonExistentId));
//     }

//     @Test
//     @DisplayName("Should get user transactions successfully")
//     void testGetUserTransactions() {
//         // Given
//         List<Transaction> transactionList = Arrays.asList(transaction);
//         Page<Transaction> transactionPage = new PageImpl<>(transactionList);
        
//         when(transactionRepository.findByBuyerOrSeller(eq(buyer), eq(buyer), any(Pageable.class)))
//             .thenReturn(transactionPage);

//         // When
//         Page<Transaction> result = transactionService.getUserTransactions(buyer, 0, 10);

//         // Then
//         assertNotNull(result);
//         assertEquals(1, result.getTotalElements());
//         verify(transactionRepository).findByBuyerOrSeller(eq(buyer), eq(buyer), any(Pageable.class));
//         verify(validationService).validateUser(buyer);
//     }

//     @Test
//     @DisplayName("Should get disputed transactions successfully")
//     void testGetDisputedTransactions() {
//         // Given
//         List<Transaction> disputedTransactions = Arrays.asList(transaction);
//         Page<Transaction> transactionPage = new PageImpl<>(disputedTransactions);
        
//         when(transactionRepository.findByStatus(eq(Transaction.TransactionStatus.DISPUTED), any(Pageable.class)))
//             .thenReturn(transactionPage);

//         // When
//         Page<Transaction> result = transactionService.getDisputedTransactions(0, 10);

//         // Then
//         assertNotNull(result);
//         assertEquals(1, result.getTotalElements());
//         verify(transactionRepository).findByStatus(eq(Transaction.TransactionStatus.DISPUTED), any(Pageable.class));
//     }

//     // ============== INTEGRATION TESTS ==============

//     @Test
//     @DisplayName("Should create dispute for transaction successfully")
//     void testCreateDisputeForTransaction() {
//         // Given
//         UUID transactionId = transaction.getId();
//         String reason = "Payment issue";
        
//         when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
//         when(disputeRepository.save(any(Dispute.class))).thenAnswer(invocation -> {
//             Dispute savedDispute = invocation.getArgument(0);
//             savedDispute.setId(UUID.randomUUID());
//             return savedDispute;
//         });
//         when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        
//         doNothing().when(validationService).validateDisputeCreationRights(any(Transaction.class), any(User.class));
//         doNothing().when(validationService).validateDisputeReason(anyString());
//         doNothing().when(validationService).validateNoExistingOpenDisputes(any(UUID.class));
//         doNothing().when(notificationService).notifyDisputeCreated(any(User.class), any(User.class), anyString());
//         doNothing().when(auditService).logDisputeCreated(anyString(), anyString());

//         // When
//         Dispute result = transactionService.createDispute(transactionId, buyer, reason);

//         // Then
//         assertNotNull(result);
//         assertEquals(reason, result.getReason());
//         assertEquals(buyer, result.getRaisedBy());
//         assertEquals(transaction, result.getTransaction());
//         assertEquals(Dispute.DisputeStatus.OPEN, result.getStatus());
        
//         verify(transactionRepository).findById(transactionId);
//         verify(disputeRepository).save(any(Dispute.class));
//         verify(transactionRepository).save(any(Transaction.class));
//         verify(validationService).validateDisputeCreationRights(transaction, buyer);
//         verify(notificationService).notifyDisputeCreated(eq(buyer), eq(seller), anyString());
//     }

//     @Test
//     @DisplayName("Should mark transaction as disputed successfully")
//     void testMarkTransactionAsDisputed() {
//         // Given
//         UUID transactionId = transaction.getId();
//         String disputeId = UUID.randomUUID().toString();
        
//         when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
//         when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        
//         doNothing().when(validationService).validateTransactionStatusChange(any(Transaction.class), any(Transaction.TransactionStatus.class));

//         // When
//         Transaction result = transactionService.markAsDisputed(transactionId, disputeId);

//         // Then
//         assertNotNull(result);
//         assertEquals(Transaction.TransactionStatus.DISPUTED, result.getStatus());
        
//         verify(transactionRepository).findById(transactionId);
//         verify(transactionRepository).save(transaction);
//         verify(validationService).validateTransactionStatusChange(transaction, Transaction.TransactionStatus.DISPUTED);
//     }

//     @Test
//     @DisplayName("Should validate integration between dispute and transaction services")
//     void testDisputeTransactionIntegration() {
//         // Given
//         UUID transactionId = transaction.getId();
//         UUID disputeId = dispute.getId();
        
//         when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
//         when(disputeRepository.findById(disputeId)).thenReturn(Optional.of(dispute));

//         // When
//         Transaction foundTransaction = transactionService.findTransactionById(transactionId);
//         Dispute foundDispute = disputeService.findDisputeById(disputeId);

//         // Then
//         assertNotNull(foundTransaction);
//         assertNotNull(foundDispute);
//         assertEquals(transactionId, foundTransaction.getId());
//         assertEquals(disputeId, foundDispute.getId());
        
//         // Verify the transaction is linked to the dispute
//         assertEquals(transactionId, foundDispute.getTransaction().getId());
//     }

//     @Test
//     @DisplayName("Should check if user can create dispute")
//     void testCanUserCreateDispute() {
//         // Given
//         UUID transactionId = transaction.getId();
        
//         when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
//         doNothing().when(validationService).validateDisputeCreationRights(any(Transaction.class), any(User.class));
//         doNothing().when(validationService).validateNoExistingOpenDisputes(any(UUID.class));

//         // When
//         boolean canCreate = disputeService.canUserCreateDispute(transactionId, buyer);

//         // Then
//         assertTrue(canCreate);
//         verify(transactionRepository).findById(transactionId);
//     }

//     @Test
//     @DisplayName("Should handle validation failure when creating dispute")
//     void testCannotCreateDisputeWhenValidationFails() {
//         // Given
//         UUID transactionId = transaction.getId();
        
//         when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
//         doThrow(new BusinessOperationException("User cannot create dispute"))
//             .when(validationService).validateDisputeCreationRights(any(Transaction.class), any(User.class));

//         // When
//         boolean canCreate = disputeService.canUserCreateDispute(transactionId, seller);

//         // Then
//         assertFalse(canCreate);
//     }

//     // ============== UTILITY METHODS ==============

//     private User createUser(String email, String username, User.UserRole role) {
//         User user = new User();
//         user.setId(UUID.randomUUID());
//         user.setEmail(email);
//         user.setUsername(username.toLowerCase().replace(" ", "_"));
//         user.setRole(role);
//         user.setCreatedAt(LocalDateTime.now());
//         return user;
//     }

//     private CarbonCredit createCarbonCredit() {
//         CarbonCredit credit = new CarbonCredit();
//         credit.setId(UUID.randomUUID());
//         credit.setCreditAmount(new BigDecimal("100.00"));
//         credit.setCo2ReducedKg(new BigDecimal("50.00"));
//         credit.setUser(seller);
//         credit.setStatus(CarbonCredit.CreditStatus.VERIFIED);
//         credit.setCreatedAt(LocalDateTime.now());
//         return credit;
//     }

//     private CreditListing createCreditListing() {
//         CreditListing listing = new CreditListing();
//         listing.setId(UUID.randomUUID());
//         listing.setCredit(credit);
//         listing.setListingType(CreditListing.ListingType.FIXED);
//         listing.setPrice(new BigDecimal("500.00"));
//         listing.setMinBid(new BigDecimal("50.00"));
//         listing.setStatus(CreditListing.ListingStatus.ACTIVE);
//         listing.setCreatedAt(LocalDateTime.now());
//         return listing;
//     }

//     private Transaction createTransaction() {
//         Transaction transaction = new Transaction();
//         transaction.setId(UUID.randomUUID());
//         transaction.setBuyer(buyer);
//         transaction.setSeller(seller);
//         transaction.setCredit(credit);
//         transaction.setListing(listing);
//         transaction.setAmount(new BigDecimal("250.00"));
//         transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
//         transaction.setCreatedAt(LocalDateTime.now());
//         return transaction;
//     }

//     private Dispute createDispute() {
//         Dispute dispute = new Dispute();
//         dispute.setId(UUID.randomUUID());
//         dispute.setTransaction(transaction);
//         dispute.setRaisedBy(buyer);
//         dispute.setReason("Payment not received");
//         dispute.setStatus(Dispute.DisputeStatus.OPEN);
//         dispute.setCreatedAt(LocalDateTime.now());
//         return dispute;
//     }
// }
