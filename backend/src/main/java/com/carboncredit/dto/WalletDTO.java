package com.carboncredit.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.carboncredit.entity.Wallet;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletDTO {
    private UUID id;
    private UUID userId;
    private String username;
    private BigDecimal creditBalance;
    private BigDecimal cashBalance;
    private LocalDateTime updatedAt;

    // Constructor from Wallet entity
    public WalletDTO(Wallet wallet) {
        this.id = wallet.getId();
        this.userId = wallet.getUser() != null ? wallet.getUser().getId() : null;
        this.username = wallet.getUser() != null ? wallet.getUser().getUsername() : null;
        this.creditBalance = wallet.getCreditBalance();
        this.cashBalance = wallet.getCashBalance();
        this.updatedAt = wallet.getUpdatedAt();
    }
}