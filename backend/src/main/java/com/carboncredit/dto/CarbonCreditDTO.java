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
    private UUID id;
    private UserDTO owner;
    private UUID journeyId;
    private BigDecimal co2ReducedKg;
    private BigDecimal creditAmount;
    private CarbonCredit.CreditStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime listedAt;

    // Constructor from CarbonCredit entity
    public CarbonCreditDTO(CarbonCredit credit) {
        this.id = credit.getId();
        this.owner = credit.getUser() != null ? new UserDTO(credit.getUser()) : null;
        this.journeyId = credit.getJourney() != null ? credit.getJourney().getId() : null;
        this.co2ReducedKg = credit.getCo2ReducedKg();
        this.creditAmount = credit.getCreditAmount();
        this.status = credit.getStatus();
        this.createdAt = credit.getCreatedAt();
        this.verifiedAt = credit.getVerifiedAt();
        this.listedAt = credit.getListedAt();
    }

    // Lightweight constructor for preventing circular references
    public CarbonCreditDTO(CarbonCredit credit, boolean lightweight) {
        this.id = credit.getId();
        if (!lightweight && credit.getUser() != null) {
            this.owner = new UserDTO();
            this.owner.setId(credit.getUser().getId());
            this.owner.setUsername(credit.getUser().getUsername());
            this.owner.setRole(credit.getUser().getRole());
        }
        this.journeyId = credit.getJourney() != null ? credit.getJourney().getId() : null;
        this.co2ReducedKg = credit.getCo2ReducedKg();
        this.creditAmount = credit.getCreditAmount();
        this.status = credit.getStatus();
        this.createdAt = credit.getCreatedAt();
        this.verifiedAt = credit.getVerifiedAt();
        this.listedAt = credit.getListedAt();
    }
}
