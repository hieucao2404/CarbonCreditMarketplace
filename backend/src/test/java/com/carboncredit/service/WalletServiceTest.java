package com.carboncredit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.carboncredit.entity.User;
import com.carboncredit.entity.Wallet;
import com.carboncredit.repository.WalletRepository;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;
    
    private WalletService walletService;
    
    private User testUser;
    private Wallet testWallet;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        walletService = new WalletService(walletRepository);
        
        // Create test user
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setRole(User.UserRole.BUYER);
        
        // Create test wallet
        testWallet = new Wallet();
        testWallet.setId(UUID.randomUUID());
        testWallet.setUser(testUser);
        testWallet.setCreditBalance(new BigDecimal("50.00"));
        testWallet.setCashBalance(new BigDecimal("100.00"));
    }
    
    @Test
    void createWalletForUser_Success() {
        // Arrange
        Wallet newWallet = new Wallet();
        newWallet.setId(UUID.randomUUID());
        newWallet.setUser(testUser);
        newWallet.setCreditBalance(BigDecimal.ZERO);
        newWallet.setCashBalance(BigDecimal.ZERO);
        
        when(walletRepository.save(any(Wallet.class))).thenReturn(newWallet);
        
        // Act
        Wallet result = walletService.createWalletForUser(testUser);
        
        // Assert
        assertNotNull(result);
        assertEquals(testUser, result.getUser());
        assertEquals(BigDecimal.ZERO, result.getCreditBalance());
        assertEquals(BigDecimal.ZERO, result.getCashBalance());
        
        verify(walletRepository).save(any(Wallet.class));
    }
    
    @Test
    void findByUserId_Found() {
        // Arrange
        when(walletRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testWallet));
        
        // Act
        Optional<Wallet> result = walletService.findByUserId(testUser.getId());
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(testWallet, result.get());
        
        verify(walletRepository).findByUserId(testUser.getId());
    }
    
    @Test
    void findByUserId_NotFound() {
        // Arrange
        UUID nonExistentUserId = UUID.randomUUID();
        when(walletRepository.findByUserId(nonExistentUserId)).thenReturn(Optional.empty());
        
        // Act
        Optional<Wallet> result = walletService.findByUserId(nonExistentUserId);
        
        // Assert
        assertFalse(result.isPresent());
        
        verify(walletRepository).findByUserId(nonExistentUserId);
    }
    
    @Test
    void getOrCreateWallet_ExistingWallet() {
        // Arrange
        when(walletRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testWallet));
        
        // Act
        Wallet result = walletService.getOrCreateWallet(testUser);
        
        // Assert
        assertEquals(testWallet, result);
        
        verify(walletRepository).findByUserId(testUser.getId());
        verify(walletRepository, never()).save(any(Wallet.class));
    }
    
    @Test
    void getOrCreateWallet_CreateNewWallet() {
        // Arrange
        Wallet newWallet = new Wallet();
        newWallet.setId(UUID.randomUUID());
        newWallet.setUser(testUser);
        newWallet.setCreditBalance(BigDecimal.ZERO);
        newWallet.setCashBalance(BigDecimal.ZERO);
        
        when(walletRepository.findByUserId(testUser.getId())).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenReturn(newWallet);
        
        // Act
        Wallet result = walletService.getOrCreateWallet(testUser);
        
        // Assert
        assertNotNull(result);
        assertEquals(testUser, result.getUser());
        assertEquals(BigDecimal.ZERO, result.getCreditBalance());
        assertEquals(BigDecimal.ZERO, result.getCashBalance());
        
        verify(walletRepository).findByUserId(testUser.getId());
        verify(walletRepository).save(any(Wallet.class));
    }
    
    @Test
    void updateCreditBalance_PositiveAmount_Success() {
        // Arrange
        BigDecimal addAmount = new BigDecimal("25.00");
        Wallet updatedWallet = new Wallet();
        updatedWallet.setId(testWallet.getId());
        updatedWallet.setUser(testUser);
        updatedWallet.setCreditBalance(new BigDecimal("75.00")); // 50 + 25
        updatedWallet.setCashBalance(testWallet.getCashBalance());
        
        when(walletRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(updatedWallet);
        
        // Act
        Wallet result = walletService.updateCreditBalance(testUser.getId(), addAmount);
        
        // Assert
        assertEquals(new BigDecimal("75.00"), result.getCreditBalance());
        
        verify(walletRepository).findByUserId(testUser.getId());
        verify(walletRepository).save(any(Wallet.class));
    }
    
    @Test
    void updateCreditBalance_NegativeAmount_Success() {
        // Arrange
        BigDecimal deductAmount = new BigDecimal("-20.00");
        Wallet updatedWallet = new Wallet();
        updatedWallet.setId(testWallet.getId());
        updatedWallet.setUser(testUser);
        updatedWallet.setCreditBalance(new BigDecimal("30.00")); // 50 - 20
        updatedWallet.setCashBalance(testWallet.getCashBalance());
        
        when(walletRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(updatedWallet);
        
        // Act
        Wallet result = walletService.updateCreditBalance(testUser.getId(), deductAmount);
        
        // Assert
        assertEquals(new BigDecimal("30.00"), result.getCreditBalance());
        
        verify(walletRepository).findByUserId(testUser.getId());
        verify(walletRepository).save(any(Wallet.class));
    }
    
    @Test
    void updateCreditBalance_InsufficientBalance_ThrowsException() {
        // Arrange
        BigDecimal deductAmount = new BigDecimal("-75.00"); // More than available 50
        
        when(walletRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testWallet));
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            walletService.updateCreditBalance(testUser.getId(), deductAmount);
        });
        
        assertEquals("Insufficient credit balance", exception.getMessage());
        
        verify(walletRepository).findByUserId(testUser.getId());
        verify(walletRepository, never()).save(any(Wallet.class));
    }
    
    @Test
    void updateCreditBalance_WalletNotFound_ThrowsException() {
        // Arrange
        UUID nonExistentUserId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("10.00");
        
        when(walletRepository.findByUserId(nonExistentUserId)).thenReturn(Optional.empty());
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            walletService.updateCreditBalance(nonExistentUserId, amount);
        });
        
        assertEquals("Wallet not found for user: " + nonExistentUserId, exception.getMessage());
        
        verify(walletRepository).findByUserId(nonExistentUserId);
        verify(walletRepository, never()).save(any(Wallet.class));
    }
    
    @Test
    void updateCashBalance_PositiveAmount_Success() {
        // Arrange
        BigDecimal addAmount = new BigDecimal("50.00");
        Wallet updatedWallet = new Wallet();
        updatedWallet.setId(testWallet.getId());
        updatedWallet.setUser(testUser);
        updatedWallet.setCreditBalance(testWallet.getCreditBalance());
        updatedWallet.setCashBalance(new BigDecimal("150.00")); // 100 + 50
        
        when(walletRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(updatedWallet);
        
        // Act
        Wallet result = walletService.updateCashBalance(testUser.getId(), addAmount);
        
        // Assert
        assertEquals(new BigDecimal("150.00"), result.getCashBalance());
        
        verify(walletRepository).findByUserId(testUser.getId());
        verify(walletRepository).save(any(Wallet.class));
    }
    
    @Test
    void updateCashBalance_NegativeAmount_Success() {
        // Arrange
        BigDecimal deductAmount = new BigDecimal("-30.00");
        Wallet updatedWallet = new Wallet();
        updatedWallet.setId(testWallet.getId());
        updatedWallet.setUser(testUser);
        updatedWallet.setCreditBalance(testWallet.getCreditBalance());
        updatedWallet.setCashBalance(new BigDecimal("70.00")); // 100 - 30
        
        when(walletRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(updatedWallet);
        
        // Act
        Wallet result = walletService.updateCashBalance(testUser.getId(), deductAmount);
        
        // Assert
        assertEquals(new BigDecimal("70.00"), result.getCashBalance());
        
        verify(walletRepository).findByUserId(testUser.getId());
        verify(walletRepository).save(any(Wallet.class));
    }
    
    @Test
    void updateCashBalance_InsufficientBalance_ThrowsException() {
        // Arrange
        BigDecimal deductAmount = new BigDecimal("-150.00"); // More than available 100
        
        when(walletRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testWallet));
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            walletService.updateCashBalance(testUser.getId(), deductAmount);
        });
        
        assertEquals("Insufficient cash balance", exception.getMessage());
        
        verify(walletRepository).findByUserId(testUser.getId());
        verify(walletRepository, never()).save(any(Wallet.class));
    }
    
    @Test
    void updateCashBalance_WalletNotFound_ThrowsException() {
        // Arrange
        UUID nonExistentUserId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("10.00");
        
        when(walletRepository.findByUserId(nonExistentUserId)).thenReturn(Optional.empty());
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            walletService.updateCashBalance(nonExistentUserId, amount);
        });
        
        assertEquals("Wallet not found for user: " + nonExistentUserId, exception.getMessage());
        
        verify(walletRepository).findByUserId(nonExistentUserId);
        verify(walletRepository, never()).save(any(Wallet.class));
    }
}