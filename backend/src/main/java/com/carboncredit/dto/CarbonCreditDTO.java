package com.carboncredit.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.carboncredit.entity.CarbonCredit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarbonCreditDTO {
   // Credit Details
    private UUID creditId;
    private String status;
    private BigDecimal co2ReducedKg;
    private BigDecimal creditAmount;
    private LocalDateTime createdAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime listedAt;

    // Related "Flat" IDs & Info
    private UUID journeyId;
    private UUID ownerId;
    private String ownerUsername;
    private UUID verifierId;
    private String verifierUsername;

    /**
     * This constructor is safe to call from your DTOMapper,
     * AS LONG AS the DTOMapper is called from your @Transactional Service.
     */
    public CarbonCreditDTO(CarbonCredit credit) {
        this.creditId = credit.getId();
        this.status = credit.getStatus().name();
        this.co2ReducedKg = credit.getCo2ReducedKg();
        this.creditAmount = credit.getCreditAmount();
        this.createdAt = credit.getCreatedAt();
        this.verifiedAt = credit.getVerifiedAt();
        this.listedAt = credit.getListedAt();

        // Safely access lazy-loaded relationships
        if (credit.getJourney() != null) {
            this.journeyId = credit.getJourney().getId();
        }

        if (credit.getUser() != null) {
            this.ownerId = credit.getUser().getId();
            this.ownerUsername = credit.getUser().getUsername();
        }
        
        if (credit.getVerifiedBy() != null) {
            this.verifierId = credit.getVerifiedBy().getId();
            this.verifierUsername = credit.getVerifiedBy().getUsername();
        }
    }
}
