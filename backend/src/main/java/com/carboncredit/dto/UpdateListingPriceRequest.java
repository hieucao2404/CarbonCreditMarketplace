package com.carboncredit.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class UpdateListingPriceRequest {
    @NotNull(message = "New price cannot be null")
    @Positive(message = "Price must be positive")
    private BigDecimal newPrice;
}
