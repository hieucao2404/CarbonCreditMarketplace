package com.carboncredit.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.carboncredit.entity.CreditListing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditListingDTO {
    // Listing Details
    private UUID listingId;
    private String status; // ACTIVE, CLOSED, CANCELLED
    private String type; // FIXED, AUCTION
    private BigDecimal price;
    private BigDecimal minBid;
    private LocalDateTime auctionEndTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Related Credit Details (Flat)
    private UUID creditId;
    private BigDecimal creditAmount; // How many credits are being sold
    private BigDecimal co2ReducedKg; // Original CO2 reduction

    // Related Seller Details (Flat)
    private UUID sellerId;
    private String sellerUsername;

    /**
     * Constructor called by DTOMapper (which is called from the Service)
     * Safely accesses lazy-loaded fields.
     */
    public CreditListingDTO(CreditListing listing) {
        this.listingId = listing.getId();
        this.status = listing.getStatus().name();
        this.type = listing.getListingType().name();
        this.price = listing.getPrice();
        this.minBid = listing.getMinBid();
        this.auctionEndTime = listing.getAuctionEndTime();
        this.createdAt = listing.getCreatedAt();
        this.updatedAt = listing.getUpdatedAt();

        if (listing.getCredit() != null) {
            this.creditId = listing.getCredit().getId();
            this.creditAmount = listing.getCredit().getCreditAmount();
            this.co2ReducedKg = listing.getCredit().getCo2ReducedKg();

            if (listing.getCredit().getUser() != null) {
                this.sellerId = listing.getCredit().getUser().getId();
                this.sellerUsername = listing.getCredit().getUser().getUsername();
            }
        }
    }
}
