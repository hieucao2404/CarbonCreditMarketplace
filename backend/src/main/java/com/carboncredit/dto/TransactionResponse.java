package com.carboncredit.dto;

import com.carboncredit.entity.Transaction;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private UUID id;
    private UUID buyerId;
    private String buyerUsername;
    private UUID sellerId;
    private String sellerUsername;
    private UUID creditId;
    private UUID listingId;
    private BigDecimal creditAmount;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public TransactionResponse(Transaction transaction) {
        this.id = transaction.getId();
        this.buyerId = transaction.getBuyer() != null ? transaction.getBuyer().getId() : null;
        this.buyerUsername = transaction.getBuyer() != null ? transaction.getBuyer().getUsername() : null;
        this.sellerId = transaction.getSeller() != null ? transaction.getSeller().getId() : null;
        this.sellerUsername = transaction.getSeller() != null ? transaction.getSeller().getUsername() : null;
        this.creditId = transaction.getCredit() != null ? transaction.getCredit().getId() : null;
        this.listingId = transaction.getListing() != null ? transaction.getListing().getId() : null;
        this.creditAmount = transaction.getCredit() != null ? transaction.getCredit().getCreditAmount() : null;
        this.totalAmount = transaction.getAmount();
        this.status = transaction.getStatus() != null ? transaction.getStatus().toString() : null;
        this.createdAt = transaction.getCreatedAt();
        this.completedAt = transaction.getCompletedAt();
    }
}
