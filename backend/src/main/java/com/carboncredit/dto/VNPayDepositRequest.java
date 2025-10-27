package com.carboncredit.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VNPayDepositRequest {
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Minimum deposit is $1.00 USD") @DecimalMax(value = "20000.0", message = "Maximum deposit is $20,000.00 USD")
    private BigDecimal amountUsd;
}
