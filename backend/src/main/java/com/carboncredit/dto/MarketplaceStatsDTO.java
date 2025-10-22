package com.carboncredit.dto;

import java.math.BigDecimal;

import com.carboncredit.service.CreditListingService;

import lombok.Data;
@Data
public class MarketplaceStatsDTO {
    private long totalActiveListings;
    private BigDecimal averagePrice;

    // Assumes MarketplaceStats is a public static inner class in
    // CreditListingService
    public MarketplaceStatsDTO(CreditListingService.MarketplaceStats stats) {
        this.totalActiveListings = stats.getTotalActiveListings();
        // Handle null average price if no listings exist
        this.averagePrice = (stats.getAveragePrice() != null) ? stats.getAveragePrice() : BigDecimal.ZERO;
    }

}
