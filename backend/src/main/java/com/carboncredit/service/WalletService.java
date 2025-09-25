package com.carboncredit.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carboncredit.entity.Wallet;
import com.carboncredit.entity.User;
import com.carboncredit.repository.WalletRepository;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
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
}
