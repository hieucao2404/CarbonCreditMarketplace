package com.carboncredit.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carboncredit.entity.Wallet;
import com.carboncredit.entity.User;
import com.carboncredit.repository.WalletRepository;

import lombok.RequiredArgsConstructor;


import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class WalletService {
    private final WalletRepository walletRepository;

    public Wallet createWalletForUser(User user) {
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setCreditBalance(BigDecimal.ZERO);
        wallet.setCashBalance(BigDecimal.ZERO);
        return walletRepository.save(wallet);
    }

    @Transactional(readOnly = true)
    public Optional<Wallet> findByUserId(UUID userId) {
        return walletRepository.findByUserId(userId);
    }
    
    public Wallet getOrCreateWallet(User user) {
        return findByUserId(user.getId())
            .orElseGet(() -> createWalletForUser(user));
    }
    
    public Wallet updateCreditBalance(UUID userId, BigDecimal amount) {
        Wallet wallet = findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("Wallet not found for user: " + userId));
        
        if (amount.compareTo(BigDecimal.ZERO) < 0 && 
            wallet.getCreditBalance().add(amount).compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Insufficient credit balance");
        }
        
        wallet.setCreditBalance(wallet.getCreditBalance().add(amount));
        return walletRepository.save(wallet);
    }
    
    public Wallet updateCashBalance(UUID userId, BigDecimal amount) {
        Wallet wallet = findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("Wallet not found for user: " + userId));
        
        if (amount.compareTo(BigDecimal.ZERO) < 0 && 
            wallet.getCashBalance().add(amount).compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Insufficient cash balance");
        }
        
        wallet.setCashBalance(wallet.getCashBalance().add(amount));
        return walletRepository.save(wallet);
    }
}
