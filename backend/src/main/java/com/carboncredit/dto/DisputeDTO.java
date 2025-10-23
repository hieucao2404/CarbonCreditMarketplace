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
    private UUID disputeId;
    private String status; // OPEN, RESOLVED, CLOSED
    private String reason;
    private String resolution;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    // Related "Flat" IDs & Info
    private UUID transactionId;
    private UUID raisedById;
    private String raisedByUsername;
    private UUID resolvedById;
    private String resolvedByUsername;
    private UUID buyerId; // From Transaction
    private String buyerUsername;
    private UUID sellerId; // From Transaction
    private String sellerUsername;

    /**
     * Constructor for DTOMapper (called from Service layer)
     */
    public DisputeDTO(Dispute dispute) {
        this.disputeId = dispute.getId();
        this.status = dispute.getStatus().name();
        this.reason = dispute.getReason();
        this.resolution = dispute.getResolution();
        this.createdAt = dispute.getCreatedAt();
        this.resolvedAt = dispute.getResolvedAt();

        if (dispute.getTransaction() != null) {
            this.transactionId = dispute.getTransaction().getId();
            // Get buyer/seller info from transaction if available
            if (dispute.getTransaction().getBuyer() != null) {
                this.buyerId = dispute.getTransaction().getBuyer().getId();
                this.buyerUsername = dispute.getTransaction().getBuyer().getUsername();
            }
            if (dispute.getTransaction().getSeller() != null) {
                this.sellerId = dispute.getTransaction().getSeller().getId();
                this.sellerUsername = dispute.getTransaction().getSeller().getUsername();
            }
        }

        if (dispute.getRaisedBy() != null) {
            this.raisedById = dispute.getRaisedBy().getId();
            this.raisedByUsername = dispute.getRaisedBy().getUsername();
        }

        if (dispute.getResolvedBy() != null) {
            this.resolvedById = dispute.getResolvedBy().getId();
            this.resolvedByUsername = dispute.getResolvedBy().getUsername();
        }
    }
}