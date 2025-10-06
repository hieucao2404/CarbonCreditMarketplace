package com.carboncredit.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BidRequest {

    @NotNull(message = "Bid amount is required")
    @Positive(message = "Bid amount must be positive")
    private BigDecimal bidAmount;
}
