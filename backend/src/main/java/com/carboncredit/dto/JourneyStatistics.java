package com.carboncredit.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JourneyStatistics {
    private int totalJourneys;
    private BigDecimal totalDistanceKm;
    private BigDecimal totalEnergyConsumedKwh;
    private BigDecimal averageDistanceKm;
    private BigDecimal totalCo2ReducedKg;
}
