package com.carboncredit.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {
    private UUID walletId;
    private UUID userId;
    private String username;
    private BigDecimal creditBalance;
    private BigDecimal cashBalance;
    private LocalDateTime lastUpdated;
}
