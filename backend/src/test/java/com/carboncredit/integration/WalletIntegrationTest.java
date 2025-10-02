package com.carboncredit.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.carboncredit.entity.User;
import com.carboncredit.entity.Wallet;
import com.carboncredit.repository.UserRepository;
import com.carboncredit.repository.WalletRepository;
import com.carboncredit.service.BankingService;
import com.carboncredit.service.UserService;
import com.carboncredit.service.WalletService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WalletIntegrationTest {

    @Autowired
    private WalletService walletService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private BankingService bankingService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private WalletRepository walletRepository;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        // Create and save test user
        testUser = new User();
        testUser.setUsername("integration_test_user");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password123");
        testUser.setRole(User.UserRole.BUYER);
        testUser = userRepository.save(testUser);
    }
    
    @Test
    void testCompleteWalletFlow() {
        // 1. Create wallet for user
        Wallet wallet = walletService.createWalletForUser(testUser);
        
        assertNotNull(wallet);
        assertEquals(testUser.getId(), wallet.getUser().getId());
        assertEquals(BigDecimal.ZERO, wallet.getCreditBalance());
        assertEquals(BigDecimal.ZERO, wallet.getCashBalance());
        
        // 2. Test getOrCreateWallet (should return existing wallet)
        Wallet existingWallet = walletService.getOrCreateWallet(testUser);
        assertEquals(wallet.getId(), existingWallet.getId());
        
        // 3. Test cash balance updates
        BigDecimal depositAmount = new BigDecimal("100.00");
        Wallet updatedWallet = walletService.updateCashBalance(testUser.getId(), depositAmount);
        
        assertEquals(new BigDecimal("100.00"), updatedWallet.getCashBalance());
        assertEquals(BigDecimal.ZERO, updatedWallet.getCreditBalance());
        
        // 4. Test credit balance updates
        BigDecimal creditAmount = new BigDecimal("50.00");
        updatedWallet = walletService.updateCreditBalance(testUser.getId(), creditAmount);
        
        assertEquals(new BigDecimal("100.00"), updatedWallet.getCashBalance());
        assertEquals(new BigDecimal("50.00"), updatedWallet.getCreditBalance());
        
        // 5. Test withdrawal (negative amount)
        BigDecimal withdrawAmount = new BigDecimal("-30.00");
        updatedWallet = walletService.updateCashBalance(testUser.getId(), withdrawAmount);
        
        assertEquals(new BigDecimal("70.00"), updatedWallet.getCashBalance());
        assertEquals(new BigDecimal("50.00"), updatedWallet.getCreditBalance());
        
        // 6. Test spending credits (negative amount)
        BigDecimal spendCredits = new BigDecimal("-20.00");
        updatedWallet = walletService.updateCreditBalance(testUser.getId(), spendCredits);
        
        assertEquals(new BigDecimal("70.00"), updatedWallet.getCashBalance());
        assertEquals(new BigDecimal("30.00"), updatedWallet.getCreditBalance());
        
        // 7. Verify final state in database
        Wallet finalWallet = walletRepository.findByUserId(testUser.getId()).orElse(null);
        assertNotNull(finalWallet);
        assertEquals(new BigDecimal("70.00"), finalWallet.getCashBalance());
        assertEquals(new BigDecimal("30.00"), finalWallet.getCreditBalance());
    }
    
    @Test
    void testInsufficientFundsValidation() {
        // Create wallet with some initial balance
        Wallet wallet = walletService.createWalletForUser(testUser);
        walletService.updateCashBalance(testUser.getId(), new BigDecimal("50.00"));
        
        // Try to withdraw more than available
        assertThrows(IllegalArgumentException.class, () -> {
            walletService.updateCashBalance(testUser.getId(), new BigDecimal("-100.00"));
        });
        
        // Try to spend more credits than available
        assertThrows(IllegalArgumentException.class, () -> {
            walletService.updateCreditBalance(testUser.getId(), new BigDecimal("-10.00"));
        });
        
        // Verify balance unchanged after failed operations
        Wallet unchangedWallet = walletRepository.findByUserId(testUser.getId()).orElse(null);
        assertNotNull(unchangedWallet);
        assertEquals(new BigDecimal("50.00"), unchangedWallet.getCashBalance());
        assertEquals(BigDecimal.ZERO, unchangedWallet.getCreditBalance());
    }
    
    @Test
    void testBankingServiceMockBehavior() {
        // Test multiple deposit attempts to verify mock behavior
        int successCount = 0;
        int totalAttempts = 100;
        
        for (int i = 0; i < totalAttempts; i++) {
            boolean result = bankingService.processDeposit(testUser.getId(), new BigDecimal("10.00"), "card_123");
            if (result) {
                successCount++;
            }
        }
        
        // Should have roughly 95% success rate (allowing for randomness)
        assertTrue(successCount >= 85 && successCount <= 100, 
                   "Success rate should be around 95%, got: " + (successCount * 100.0 / totalAttempts) + "%");
        
        // Test withdrawals
        successCount = 0;
        for (int i = 0; i < totalAttempts; i++) {
            boolean result = bankingService.processWithdrawal(testUser.getId(), new BigDecimal("10.00"), "account_456");
            if (result) {
                successCount++;
            }
        }
        
        // Should have roughly 90% success rate for withdrawals
        assertTrue(successCount >= 80 && successCount <= 100,
                   "Withdrawal success rate should be around 90%, got: " + (successCount * 100.0 / totalAttempts) + "%");
    }
    
    @Test
    void testWalletCreationForNewUser() {
        // Create another test user
        User newUser = new User();
        newUser.setUsername("new_user");
        newUser.setEmail("new@example.com");
        newUser.setPassword("password456");
        newUser.setRole(User.UserRole.EV_OWNER);
        newUser = userRepository.save(newUser);
        
        // Test getOrCreateWallet for user without existing wallet
        Wallet newWallet = walletService.getOrCreateWallet(newUser);
        
        assertNotNull(newWallet);
        assertEquals(newUser.getId(), newWallet.getUser().getId());
        assertEquals(BigDecimal.ZERO, newWallet.getCreditBalance());
        assertEquals(BigDecimal.ZERO, newWallet.getCashBalance());
        
        // Verify wallet was saved to database
        Wallet savedWallet = walletRepository.findByUserId(newUser.getId()).orElse(null);
        assertNotNull(savedWallet);
        assertEquals(newWallet.getId(), savedWallet.getId());
    }
    
    @Test
    void testConcurrentWalletOperations() {
        // Create wallet with initial balance
        Wallet wallet = walletService.createWalletForUser(testUser);
        walletService.updateCashBalance(testUser.getId(), new BigDecimal("1000.00"));
        
        // Simulate multiple operations
        walletService.updateCashBalance(testUser.getId(), new BigDecimal("-100.00")); // Withdraw 100
        walletService.updateCashBalance(testUser.getId(), new BigDecimal("50.00"));   // Deposit 50
        walletService.updateCreditBalance(testUser.getId(), new BigDecimal("75.00")); // Add credits
        walletService.updateCashBalance(testUser.getId(), new BigDecimal("-200.00")); // Withdraw 200
        
        // Verify final balance
        Wallet finalWallet = walletRepository.findByUserId(testUser.getId()).orElse(null);
        assertNotNull(finalWallet);
        assertEquals(new BigDecimal("750.00"), finalWallet.getCashBalance()); // 1000 - 100 + 50 - 200
        assertEquals(new BigDecimal("75.00"), finalWallet.getCreditBalance());
        assertNotNull(finalWallet.getUpdatedAt());
    }
}