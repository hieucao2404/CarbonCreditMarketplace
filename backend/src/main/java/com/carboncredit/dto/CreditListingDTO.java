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
  private UUID id;
    private CarbonCreditDTO credit;
    private CreditListing.ListingType listingType;
    private BigDecimal price;
    private BigDecimal minBid;
    private LocalDateTime auctionEndTime;
    private CreditListing.ListingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CreditListingDTO(CreditListing listing) {
        this.id = listing.getId();
        this.credit = listing.getCredit() != null ? new CarbonCreditDTO(listing.getCredit(), true) : null; // Use lightweight constructor
        this.listingType = listing.getListingType();
        this.price = listing.getPrice();
        this.minBid = listing.getMinBid();
        this.auctionEndTime = listing.getAuctionEndTime();
        this.status = listing.getStatus();
        this.createdAt = listing.getCreatedAt();
        this.updatedAt = listing.getUpdatedAt();
    }

    // Lightweight constructor for preventing circular references
    public CreditListingDTO(CreditListing listing, boolean lightweight) {
        this.id = listing.getId();
        if (!lightweight && listing.getCredit() != null) {
            this.credit = new CarbonCreditDTO(listing.getCredit(), true);
        }
        this.listingType = listing.getListingType();
        this.price = listing.getPrice();
        this.minBid = listing.getMinBid();
        this.auctionEndTime = listing.getAuctionEndTime();
        this.status = listing.getStatus();
        this.createdAt = listing.getCreatedAt();
        this.updatedAt = listing.getUpdatedAt();
    }
}
