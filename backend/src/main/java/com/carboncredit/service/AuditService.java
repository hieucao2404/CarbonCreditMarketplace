package com.carboncredit.service;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.carboncredit.entity.AuditLog;
import com.carboncredit.entity.CarbonCredit;
import com.carboncredit.entity.User;
import com.carboncredit.repository.AuditLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditService {
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public void logTransactionInitiated(String transactionId, String buyerId, String sellerId) {
        log.info("AUDIT: Transaction initiated - ID: {}, Buyer: {}, Seller: {}", transactionId, buyerId, sellerId);
    }

    public void logTransactionInitiated(String transactionId, String buyerId, String sellerId, String amount) {
        log.info("AUDIT: Transaction initiated - ID: {}, Buyer: {}, Seller: {}, Amount: {}", 
            transactionId, buyerId, sellerId, amount);
    }

    public void logTransactionCompleted(String transactionId, String buyerId, String sellerId) {
        log.info("AUDIT: Transaction completed - ID: {}, Buyer: {}, Seller: {}", 
            transactionId, buyerId, sellerId);
    }

    public void logTransactionFailed(String transactionId, String reason) {
        log.info("AUDIT: Transaction failed - ID: {}, Reason: {}", transactionId, reason);
    }

    public void logDisputeCreated(String disputeId, String transactionId) {
        log.info("AUDIT: Dispute created - ID: {}, Transaction: {}", disputeId, transactionId);
    }

    public void logDisputeResolved(String disputeId, String resolution) {
        log.info("AUDIT: Dispute resolved - ID: {}, Resolution: {}", disputeId, resolution);
    }

    // Persist verification audit entry
    public void logVerification(CarbonCredit credit, User verifier, BigDecimal beforeAmount, BigDecimal afterAmount, String comments) {
        //Build comments/metadata string(you can expand to structure fields later)
        String msg = (comments == null ? "" : comments);
        msg = msg + String.format("| creditBefore=%s, creditAfter=%s", beforeAmount, afterAmount);

        //Persisist AuditLog entity
        AuditLog entry = new AuditLog();
        entry.setCredit(credit);
        entry.setVerifier(verifier);
        entry.setAction(AuditLog.AuditAction.VERIFIED);
        entry.setComments(msg);

        auditLogRepository.save(entry);

        //logging for convenience
        log.info("AUDIT: Credit {} VERIFIED by {} (before={}, after={}) - comments: {}", credit.getId(), verifier.getUsername(), beforeAmount, afterAmount, comments);

    }

    // persist rejection audit entry
    public void logRejection(CarbonCredit credit, User verifier, String comments) {
        AuditLog entry = new AuditLog();
        entry.setCredit(credit);
        entry.setVerifier(verifier);
        entry.setAction(AuditLog.AuditAction.REJECTED);
        entry.setComments(comments);

        auditLogRepository.save(entry);
        log.info("AUDIT: Credit {} REJECTED by {} - comments: {}", credit.getId(), verifier.getUsername(), comments);
    }
}
