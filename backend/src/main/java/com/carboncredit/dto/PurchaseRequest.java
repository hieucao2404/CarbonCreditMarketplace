package com.carboncredit.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequest {
    private UUID listingId;
    private BigDecimal offerPrice;
    private String paymentMethodId;
    private String notes;
}
