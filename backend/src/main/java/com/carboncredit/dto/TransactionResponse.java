package com.carboncredit.dto;

import com.carboncredit.entity.Transaction;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class TransactionResponse {
    private UUID id;
    private UUID buyerId;
    private String buyerUsername;
    private UUID sellerId;
    private String sellerUsername;
    private UUID creditId;
    private BigDecimal creditAmount;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public TransactionResponse(Transaction transaction) {
        this.id = transaction.getId();
        this.buyerId = transaction.getBuyer().getId();
        this.buyerUsername = transaction.getBuyer().getUsername();
        this.sellerId = transaction.getListing().getCredit().getUser().getId();
        this.sellerUsername = transaction.getListing().getCredit().getUser().getUsername();
        this.creditId = transaction.getListing().getCredit().getId();
        this.creditAmount = transaction.getListing().getCredit().getCreditAmount();
        this.totalAmount = transaction.getAmount();
        this.status = transaction.getStatus().toString();
        this.createdAt = transaction.getCreatedAt();
        this.completedAt = transaction.getCompletedAt();
    }
}
