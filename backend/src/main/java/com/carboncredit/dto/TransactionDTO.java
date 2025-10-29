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
    
    // ✅ ADD THESE FIELDS
    private BigDecimal carbonCreditsAmount; // Amount of credits transferred
    private BigDecimal totalPrice; // Total price paid
    private BigDecimal amount; // Keep for backward compatibility
    
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    // Constructor from Transaction entity
    public TransactionDTO(Transaction transaction) {
        this.id = transaction.getId();
        
        // User info
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
        
        // Credit info (basic to avoid circular references)
        if (transaction.getCredit() != null) {
            this.credit = new CarbonCreditDTO();
            this.credit.setCreditId(transaction.getCredit().getId());
            this.credit.setCreditAmount(transaction.getCredit().getCreditAmount());
        }
        
        // Listing info (basic to avoid circular references)
        if (transaction.getListing() != null) {
            this.listing = new CreditListingDTO();
            this.listing.setListingId(transaction.getListing().getId());
            this.listing.setPrice(transaction.getListing().getPrice());
        }
        
        // ✅ Calculate credit amount and total price from listing
        if (transaction.getListing() != null && transaction.getCredit() != null) {
            this.carbonCreditsAmount = transaction.getCredit().getCreditAmount();
            this.totalPrice = transaction.getListing().getPrice()
                .multiply(this.carbonCreditsAmount);
        } else if (transaction.getCredit() != null) {
            this.carbonCreditsAmount = transaction.getCredit().getCreditAmount();
        }
        
        this.amount = transaction.getAmount();
        this.status = transaction.getStatus() != null 
            ? transaction.getStatus().toString() 
            : null;
        this.createdAt = transaction.getCreatedAt();
        this.completedAt = transaction.getCompletedAt();
    }
}
