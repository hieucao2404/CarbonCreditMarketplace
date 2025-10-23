// package com.carboncredit.controller;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.*;
// import static org.mockito.Mockito.*;

// import java.math.BigDecimal;
// import java.time.LocalDateTime;
// import java.util.Optional;
// import java.util.UUID;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.Mock;
// import org.mockito.MockitoAnnotations;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.data.domain.Page;
// import org.springframework.data.domain.PageImpl;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.core.Authentication;

// import com.carboncredit.dto.DepositRequest;
// import com.carboncredit.dto.TransactionDTO;
// import com.carboncredit.dto.WalletResponse;
// import com.carboncredit.dto.WithdrawRequest;
// import com.carboncredit.entity.Transaction;
// import com.carboncredit.entity.User;
// import com.carboncredit.entity.Wallet;
// import com.carboncredit.service.BankingService;
// import com.carboncredit.service.TransactionService;
// import com.carboncredit.service.UserService;
// import com.carboncredit.service.WalletService;

// import java.util.Arrays;

// @ExtendWith(MockitoExtension.class)
// class WalletControllerTest {

//     @Mock
//     private WalletService walletService;
    
//     @Mock
//     private UserService userService;
    
//     @Mock
//     private BankingService bankingService;
    
//     @Mock
//     private TransactionService transactionService;
    
//     @Mock
//     private Authentication authentication;
    
//     private WalletController walletController;
    
//     private User testUser;
//     private User adminUser;
//     private Wallet testWallet;
    
//     @BeforeEach
//     void setUp() {
//         MockitoAnnotations.openMocks(this);
//         walletController = new WalletController(walletService, userService, bankingService, transactionService);
        
//         // Create test user
//         testUser = new User();
//         testUser.setId(UUID.randomUUID());
//         testUser.setUsername("testuser");
//         testUser.setRole(User.UserRole.BUYER);
        
//         // Create admin user
//         adminUser = new User();
//         adminUser.setId(UUID.randomUUID());
//         adminUser.setUsername("admin");
//         adminUser.setRole(User.UserRole.ADMIN);
        
//         // Create test wallet
//         testWallet = new Wallet();
//         testWallet.setId(UUID.randomUUID());
//         testWallet.setUser(testUser);
//         testWallet.setCreditBalance(new BigDecimal("50.00"));
//         testWallet.setCashBalance(new BigDecimal("100.00"));
//         testWallet.setUpdatedAt(LocalDateTime.now());
//     }
    
//     @Test
//     void getMyWallet_Success() {
//         // Arrange
//         when(authentication.getName()).thenReturn("testuser");
//         when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
//         when(walletService.getOrCreateWallet(testUser)).thenReturn(testWallet);
        
//         // Act
//         ResponseEntity<WalletResponse> response = walletController.getMyWallet(authentication);
        
//         // Assert
//         assertEquals(HttpStatus.OK, response.getStatusCode());
//         assertNotNull(response.getBody());
//         assertEquals(testWallet.getId(), response.getBody().getWalletId());
//         assertEquals(testUser.getId(), response.getBody().getUserId());
//         assertEquals("testuser", response.getBody().getUsername());
//         assertEquals(new BigDecimal("50.00"), response.getBody().getCreditBalance());
//         assertEquals(new BigDecimal("100.00"), response.getBody().getCashBalance());
        
//         verify(userService).findByUsername("testuser");
//         verify(walletService).getOrCreateWallet(testUser);
//     }
    
//     @Test
//     void getMyWallet_UserNotFound() {
//         // Arrange
//         when(authentication.getName()).thenReturn("nonexistent");
//         when(userService.findByUsername("nonexistent")).thenReturn(Optional.empty());
        
//         // Act
//         ResponseEntity<WalletResponse> response = walletController.getMyWallet(authentication);
        
//         // Assert
//         assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
//         assertNull(response.getBody());
//     }
    
//     @Test
//     void checkSufficientBalance_CashSufficient() {
//         // Arrange
//         BigDecimal amount = new BigDecimal("50.00");
//         when(authentication.getName()).thenReturn("testuser");
//         when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
//         when(walletService.getOrCreateWallet(testUser)).thenReturn(testWallet);
        
//         // Act
//         ResponseEntity<Boolean> response = walletController.checkSufficientBalance(amount, "CASH", authentication);
        
//         // Assert
//         assertEquals(HttpStatus.OK, response.getStatusCode());
//         assertTrue(response.getBody());
//     }
    
//     @Test
//     void checkSufficientBalance_CashInsufficient() {
//         // Arrange
//         BigDecimal amount = new BigDecimal("150.00"); // More than available $100
//         when(authentication.getName()).thenReturn("testuser");
//         when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
//         when(walletService.getOrCreateWallet(testUser)).thenReturn(testWallet);
        
//         // Act
//         ResponseEntity<Boolean> response = walletController.checkSufficientBalance(amount, "CASH", authentication);
        
//         // Assert
//         assertEquals(HttpStatus.OK, response.getStatusCode());
//         assertFalse(response.getBody());
//     }
    
//     @Test
//     void checkSufficientBalance_CreditSufficient() {
//         // Arrange
//         BigDecimal amount = new BigDecimal("30.00");
//         when(authentication.getName()).thenReturn("testuser");
//         when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
//         when(walletService.getOrCreateWallet(testUser)).thenReturn(testWallet);
        
//         // Act
//         ResponseEntity<Boolean> response = walletController.checkSufficientBalance(amount, "CREDIT", authentication);
        
//         // Assert
//         assertEquals(HttpStatus.OK, response.getStatusCode());
//         assertTrue(response.getBody());
//     }
    
//     @Test
//     void checkSufficientBalance_CreditInsufficient() {
//         // Arrange
//         BigDecimal amount = new BigDecimal("75.00"); // More than available 50 credits
//         when(authentication.getName()).thenReturn("testuser");
//         when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
//         when(walletService.getOrCreateWallet(testUser)).thenReturn(testWallet);
        
//         // Act
//         ResponseEntity<Boolean> response = walletController.checkSufficientBalance(amount, "CREDIT", authentication);
        
//         // Assert
//         assertEquals(HttpStatus.OK, response.getStatusCode());
//         assertFalse(response.getBody());
//     }
    
//     @Test
//     void depositFunds_Success() {
//         // Arrange
//         DepositRequest request = new DepositRequest(new BigDecimal("50.00"), "card_123", "Test deposit");
//         Wallet updatedWallet = new Wallet();
//         updatedWallet.setId(testWallet.getId());
//         updatedWallet.setUser(testUser);
//         updatedWallet.setCreditBalance(new BigDecimal("50.00"));
//         updatedWallet.setCashBalance(new BigDecimal("150.00")); // 100 + 50
//         updatedWallet.setUpdatedAt(LocalDateTime.now());
        
//         when(authentication.getName()).thenReturn("testuser");
//         when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
//         when(bankingService.processDeposit(testUser.getId(), request.getAmount(), "card_123")).thenReturn(true);
//         when(walletService.updateCashBalance(testUser.getId(), request.getAmount())).thenReturn(updatedWallet);
        
//         // Act
//         ResponseEntity<WalletResponse> response = walletController.depositFunds(request, authentication);
        
//         // Assert
//         assertEquals(HttpStatus.OK, response.getStatusCode());
//         assertNotNull(response.getBody());
//         assertEquals(new BigDecimal("150.00"), response.getBody().getCashBalance());
        
//         verify(bankingService).processDeposit(testUser.getId(), request.getAmount(), "card_123");
//         verify(walletService).updateCashBalance(testUser.getId(), request.getAmount());
//     }
    
//     @Test
//     void depositFunds_BankingFailure() {
//         // Arrange
//         DepositRequest request = new DepositRequest(new BigDecimal("50.00"), "card_123", "Test deposit");
        
//         when(authentication.getName()).thenReturn("testuser");
//         when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
//         when(bankingService.processDeposit(testUser.getId(), request.getAmount(), "card_123")).thenReturn(false);
        
//         // Act
//         ResponseEntity<WalletResponse> response = walletController.depositFunds(request, authentication);
        
//         // Assert
//         assertEquals(HttpStatus.PAYMENT_REQUIRED, response.getStatusCode());
//         assertNull(response.getBody());
        
//         verify(bankingService).processDeposit(testUser.getId(), request.getAmount(), "card_123");
//         verify(walletService, never()).updateCashBalance(any(), any());
//     }
    
//     @Test
//     void withdrawFunds_Success() {
//         // Arrange
//         WithdrawRequest request = new WithdrawRequest(new BigDecimal("30.00"), "1234567890", "Test withdrawal");
//         Wallet updatedWallet = new Wallet();
//         updatedWallet.setId(testWallet.getId());
//         updatedWallet.setUser(testUser);
//         updatedWallet.setCreditBalance(new BigDecimal("50.00"));
//         updatedWallet.setCashBalance(new BigDecimal("70.00")); // 100 - 30
//         updatedWallet.setUpdatedAt(LocalDateTime.now());
        
//         when(authentication.getName()).thenReturn("testuser");
//         when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
//         when(walletService.getOrCreateWallet(testUser)).thenReturn(testWallet);
//         when(bankingService.processWithdrawal(testUser.getId(), request.getAmount(), "1234567890")).thenReturn(true);
//         when(walletService.updateCashBalance(testUser.getId(), request.getAmount().negate())).thenReturn(updatedWallet);
        
//         // Act
//         ResponseEntity<WalletResponse> response = walletController.withdrawFunds(request, authentication);
        
//         // Assert
//         assertEquals(HttpStatus.OK, response.getStatusCode());
//         assertNotNull(response.getBody());
//         assertEquals(new BigDecimal("70.00"), response.getBody().getCashBalance());
        
//         verify(bankingService).processWithdrawal(testUser.getId(), request.getAmount(), "1234567890");
//         verify(walletService).updateCashBalance(testUser.getId(), request.getAmount().negate());
//     }
    
//     @Test
//     void withdrawFunds_InsufficientBalance() {
//         // Arrange
//         WithdrawRequest request = new WithdrawRequest(new BigDecimal("150.00"), "1234567890", "Test withdrawal");
        
//         when(authentication.getName()).thenReturn("testuser");
//         when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
//         when(walletService.getOrCreateWallet(testUser)).thenReturn(testWallet);
        
//         // Act
//         ResponseEntity<WalletResponse> response = walletController.withdrawFunds(request, authentication);
        
//         // Assert
//         assertEquals(HttpStatus.PAYMENT_REQUIRED, response.getStatusCode());
//         assertNull(response.getBody());
        
//         verify(bankingService, never()).processWithdrawal(any(), any(), any());
//         verify(walletService, never()).updateCashBalance(any(), any());
//     }
    
//     @Test
//     void withdrawFunds_BankingFailure() {
//         // Arrange
//         WithdrawRequest request = new WithdrawRequest(new BigDecimal("30.00"), "1234567890", "Test withdrawal");
        
//         when(authentication.getName()).thenReturn("testuser");
//         when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
//         when(walletService.getOrCreateWallet(testUser)).thenReturn(testWallet);
//         when(bankingService.processWithdrawal(testUser.getId(), request.getAmount(), "1234567890")).thenReturn(false);
        
//         // Act
//         ResponseEntity<WalletResponse> response = walletController.withdrawFunds(request, authentication);
        
//         // Assert
//         assertEquals(HttpStatus.PAYMENT_REQUIRED, response.getStatusCode());
//         assertNull(response.getBody());
        
//         verify(bankingService).processWithdrawal(testUser.getId(), request.getAmount(), "1234567890");
//         verify(walletService, never()).updateCashBalance(any(), any());
//     }
    
//     @Test
//     void getWalletTransactions_Success() {
//         // Arrange
//         Transaction transaction1 = new Transaction();
//         transaction1.setId(UUID.randomUUID());
        
//         Transaction transaction2 = new Transaction();
//         transaction2.setId(UUID.randomUUID());
        
//         Page<Transaction> mockPage = new PageImpl<>(Arrays.asList(transaction1, transaction2));
        
//         when(authentication.getName()).thenReturn("testuser");
//         when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
//         when(transactionService.getUserTransactions(testUser, 0, 10)).thenReturn(mockPage);
        
//         // Act
//         ResponseEntity<Page<TransactionDTO>> response = walletController.getWalletTransactions(0, 10, authentication);
        
//         // Assert
//         assertEquals(HttpStatus.OK, response.getStatusCode());
//         assertNotNull(response.getBody());
//         assertEquals(2, response.getBody().getContent().size());
        
//         verify(transactionService).getUserTransactions(testUser, 0, 10);
//     }
    
//     @Test
//     void getUserWallet_AdminAccess_Success() {
//         // Arrange
//         UUID targetUserId = testUser.getId();
//         when(authentication.getName()).thenReturn("admin");
//         when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
//         when(userService.findById(targetUserId)).thenReturn(Optional.of(testUser));
//         when(walletService.getOrCreateWallet(testUser)).thenReturn(testWallet);
        
//         // Act
//         ResponseEntity<WalletResponse> response = walletController.getUserWallet(targetUserId, authentication);
        
//         // Assert
//         assertEquals(HttpStatus.OK, response.getStatusCode());
//         assertNotNull(response.getBody());
//         assertEquals(testUser.getId(), response.getBody().getUserId());
//         assertEquals("testuser", response.getBody().getUsername());
//     }
    
//     @Test
//     void getUserWallet_NonAdminAccess_Forbidden() {
//         // Arrange
//         UUID targetUserId = UUID.randomUUID();
//         when(authentication.getName()).thenReturn("testuser");
//         when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
//         // Act
//         ResponseEntity<WalletResponse> response = walletController.getUserWallet(targetUserId, authentication);
        
//         // Assert
//         assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
//         assertNull(response.getBody());
//     }
    
//     @Test
//     void updateUserBalance_AdminAccess_Success() {
//         // Arrange
//         UUID targetUserId = testUser.getId();
//         BigDecimal creditAmount = new BigDecimal("10.00");
//         BigDecimal cashAmount = new BigDecimal("20.00");
//         String reason = "Admin adjustment";
        
//         Wallet updatedWallet = new Wallet();
//         updatedWallet.setId(testWallet.getId());
//         updatedWallet.setUser(testUser);
//         updatedWallet.setCreditBalance(new BigDecimal("60.00")); // 50 + 10
//         updatedWallet.setCashBalance(new BigDecimal("120.00")); // 100 + 20
//         updatedWallet.setUpdatedAt(LocalDateTime.now());
        
//         when(authentication.getName()).thenReturn("admin");
//         when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
//         when(userService.findById(targetUserId)).thenReturn(Optional.of(testUser));
//         when(walletService.updateCashBalance(targetUserId, cashAmount)).thenReturn(updatedWallet);
        
//         // Act
//         ResponseEntity<WalletResponse> response = walletController.updateUserBalance(
//             targetUserId, creditAmount, cashAmount, reason, authentication);
        
//         // Assert
//         assertEquals(HttpStatus.OK, response.getStatusCode());
//         assertNotNull(response.getBody());
//         assertEquals(new BigDecimal("120.00"), response.getBody().getCashBalance());
        
//         verify(walletService).updateCreditBalance(targetUserId, creditAmount);
//         verify(walletService).updateCashBalance(targetUserId, cashAmount);
//     }
    
//     @Test
//     void updateUserBalance_NonAdminAccess_Forbidden() {
//         // Arrange
//         UUID targetUserId = UUID.randomUUID();
//         BigDecimal creditAmount = new BigDecimal("10.00");
//         BigDecimal cashAmount = new BigDecimal("20.00");
        
//         when(authentication.getName()).thenReturn("testuser");
//         when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
//         // Act
//         ResponseEntity<WalletResponse> response = walletController.updateUserBalance(
//             targetUserId, creditAmount, cashAmount, "test", authentication);
        
//         // Assert
//         assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
//         assertNull(response.getBody());
        
//         verify(walletService, never()).updateCreditBalance(any(), any());
//         verify(walletService, never()).updateCashBalance(any(), any());
//     }
// }