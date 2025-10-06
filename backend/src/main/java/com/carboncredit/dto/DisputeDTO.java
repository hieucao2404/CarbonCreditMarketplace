package com.carboncredit.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.carboncredit.entity.Dispute;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisputeDTO {
    private UUID id;
    private UUID transactionId;
    private UUID raisedById;
    private String raisedByUsername;
    private UUID resolvedById;
    private String resolvedByUsername;
    private String reason;
    private String status;
    private String resolution;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    // Constructor from Dispute entity
    public DisputeDTO(Dispute dispute) {
        this.id = dispute.getId();
        this.transactionId = dispute.getTransaction() != null ? dispute.getTransaction().getId() : null;
        this.raisedById = dispute.getRaisedBy() != null ? dispute.getRaisedBy().getId() : null;
        this.raisedByUsername = dispute.getRaisedBy() != null ? dispute.getRaisedBy().getUsername() : null;
        this.resolvedById = dispute.getResolvedBy() != null ? dispute.getResolvedBy().getId() : null;
        this.resolvedByUsername = dispute.getResolvedBy() != null ? dispute.getResolvedBy().getUsername() : null;
        this.reason = dispute.getReason();
        this.status = dispute.getStatus() != null ? dispute.getStatus().toString() : null;
        this.resolution = dispute.getResolution();
        this.createdAt = dispute.getCreatedAt();
        this.resolvedAt = dispute.getResolvedAt();
    }
}