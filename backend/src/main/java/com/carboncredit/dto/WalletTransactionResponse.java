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
public class WalletTransactionResponse {
    private UUID transactionId;
    private String type; // Deposit, withdrawal, purchase, sale
    private BigDecimal amount;
    private String description;
    private LocalDateTime timestamp;
    private String status;
    private BigDecimal balanceAfter;

}
