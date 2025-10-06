package com.carboncredit.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.carboncredit.entity.Transaction;

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

    // Constructor from Transaction entity
    public WalletTransactionResponse(Transaction transaction, String transactionType) {
        this.transactionId = transaction.getId();
        this.type = transactionType;
        this.amount = transaction.getAmount();
        this.timestamp = transaction.getCreatedAt();
        this.status = transaction.getStatus() != null ? transaction.getStatus().toString() : "UNKNOWN";
        
        // Generate description based on transaction type
        if (transaction.getBuyer() != null && transaction.getSeller() != null) {
            this.description = String.format("%s carbon credits from %s", 
                transactionType.toLowerCase(), 
                transaction.getSeller().getUsername());
        } else {
            this.description = transactionType + " transaction";
        }
    }
}
