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
public class TransactionDTO {
    private UUID id;
    private UserDTO buyer;
    private UserDTO seller;
    private CarbonCreditDTO credit;
    private CreditListingDTO listing;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    // Constructor from Transaction entity - prevents circular references
    public TransactionDTO(Transaction transaction) {
        this.id = transaction.getId();
        
        // Only include basic user info to prevent circular references
        if (transaction.getBuyer() != null) {
            this.buyer = new UserDTO();
            this.buyer.setId(transaction.getBuyer().getId());
            this.buyer.setUsername(transaction.getBuyer().getUsername());
            this.buyer.setRole(transaction.getBuyer().getRole());
        }
        
        if (transaction.getSeller() != null) {
            this.seller = new UserDTO();
            this.seller.setId(transaction.getSeller().getId());
            this.seller.setUsername(transaction.getSeller().getUsername());
            this.seller.setRole(transaction.getSeller().getRole());
        }
        
        // Include basic credit info without deep relationships
        if (transaction.getCredit() != null) {
            this.credit = new CarbonCreditDTO(transaction.getCredit()); // Use lightweight constructor
        }
        
        // Include basic listing info without deep relationships
        if (transaction.getListing() != null) {
            this.listing = new CreditListingDTO(transaction.getListing()); // Use lightweight constructor
        }
        
        this.amount = transaction.getAmount();
        this.status = transaction.getStatus() != null ? transaction.getStatus().toString() : null;
        this.createdAt = transaction.getCreatedAt();
        this.completedAt = transaction.getCompletedAt();
    }
}