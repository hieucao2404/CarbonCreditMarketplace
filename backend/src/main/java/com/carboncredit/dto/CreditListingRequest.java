package com.carboncredit.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.carboncredit.entity.CreditListing.ListingType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditListingRequest {

    @NotNull(message = "Credit ID is required")
    private UUID creditId;

    @NotNull(message = "Listing type is required")
    private ListingType listingType;

    @Positive(message   = "Price must be positive")
    private BigDecimal price;

    @Positive(message = "Minimum bid must be positive")
    private BigDecimal minBid;

    private LocalDateTime auctionEndTime;
}
