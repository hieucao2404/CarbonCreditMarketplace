package com.carboncredit.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.carboncredit.entity.AuditLog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDTO {
    private UUID id;
    private UUID creditId;
    private UUID verifierId;
    private String verifierUsername;
    private String action;
    private String comments;
    private LocalDateTime createdAt;

    // Constructor from AuditLog entity
    public AuditLogDTO(AuditLog auditLog) {
        this.id = auditLog.getId();
        this.creditId = auditLog.getCredit() != null ? auditLog.getCredit().getId() : null;
        this.verifierId = auditLog.getVerifier() != null ? auditLog.getVerifier().getId() : null;
        this.verifierUsername = auditLog.getVerifier() != null ? auditLog.getVerifier().getUsername() : null;
        this.action = auditLog.getAction() != null ? auditLog.getAction().toString() : null;
        this.comments = auditLog.getComments();
        this.createdAt = auditLog.getCreatedAt();
    }
}