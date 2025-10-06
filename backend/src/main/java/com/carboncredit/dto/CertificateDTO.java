package com.carboncredit.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.carboncredit.entity.Certificate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificateDTO {
    private UUID id;
    private UUID transactionId;
    private UUID buyerId;
    private String buyerUsername;
    private UUID creditId;
    private LocalDateTime issueDate;
    private BigDecimal co2ReducedKg;
    private String certificateCode;

    // Constructor from Certificate entity
    public CertificateDTO(Certificate certificate) {
        this.id = certificate.getId();
        this.transactionId = certificate.getTransaction() != null ? certificate.getTransaction().getId() : null;
        this.buyerId = certificate.getBuyer() != null ? certificate.getBuyer().getId() : null;
        this.buyerUsername = certificate.getBuyer() != null ? certificate.getBuyer().getUsername() : null;
        this.creditId = certificate.getCredit() != null ? certificate.getCredit().getId() : null;
        this.issueDate = certificate.getIssueDate();
        this.co2ReducedKg = certificate.getCo2ReducedKg();
        this.certificateCode = certificate.getCertificateCode();
    }
}