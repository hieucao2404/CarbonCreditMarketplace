package com.carboncredit.service;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.carboncredit.entity.AuditLog;
import com.carboncredit.entity.CarbonCredit;
import com.carboncredit.entity.User;
import com.carboncredit.repository.AuditLogRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    // ======================== Carbon Credit audit methods ===========
    /**
     * Log when a journey is submitted for verification
     * Called by JourneyDataService when a new journey is created
     */
    public void logSubmission(CarbonCredit credit, User submitter) {
        AuditLog entry = new AuditLog();
        entry.setCredit(credit);
        entry.setVerifier(submitter);
        entry.setAction(AuditLog.AuditAction.SUBMITTED);

        entry.setComments("Journeys submitted for CVA verifications");

        auditLogRepository.save(entry);

        log.info("AUDIT: Credit {} SUBMITTED by {} for verification", credit.getId(),
        
        submitter.getUsername());
    }

    /**
     * Log when a CVA verifies and approve a journey
     * Called by CVAService when approving a journey
     * 
     */
    public void logVerification(CarbonCredit credit, User verifier, BigDecimal beforeAmount, BigDecimal afterAmount, String comments){
        String msg = (comments == null ? "" : comments);
        msg = msg + String.format(" | Wallet: before = %s, after=%s", beforeAmount, afterAmount);

        AuditLog entry = new AuditLog();
        entry.setCredit(credit);
        entry.setVerifier(verifier);
        entry.setAction(AuditLog.AuditAction.VERIFIED);
        entry.setComments(msg);

        auditLogRepository.save(entry);

        log.info("AUDIT: Credit {} VERIFIED by {} (wallet: {} -> {}) - {}", credit.getId(), verifier.getUsername(), beforeAmount, afterAmount);
    }

    /**
     * Log when a CVA rejects a journey
     * Called by CVA Service when rejecting a journey
     */
    public void logRejection(CarbonCredit credit, User verifier, String comments) {
    AuditLog entry = new AuditLog();
        entry.setCredit(credit);
        entry.setVerifier(verifier);
        entry.setAction(AuditLog.AuditAction.REJECTED);
        entry.setComments(comments);
        
        auditLogRepository.save(entry);
        
        log.warn("AUDIT: Credit {} REJECTED by {} - Reason: {}", 
                credit.getId(), verifier.getUsername(), comments);
    }

    // ============ TRANSACITON AUDIT METHOD FOR FUTER USE =========

    /* Log transaction initiation (marketplace trading) */
    public void logTransactionInitiated(String transactionId, String buyerId, String sellerId) {
        log.info("AUDIT: Transaction {} initiated -  Buyer: {}, Seller: {}", transactionId, buyerId, sellerId);
    }

    /** 
     * Log transaction completion
     */
    public void logTransactionCompleted(String transactionId, String buyerId, String sellerId) {
        log.info("Audit: Transaction {} completed - Buyer: {}, seller: {}", transactionId, buyerId, sellerId);
    }

    /**
     * Log transaction failure
     */
    public void logTransactionFailed(String transactionId, String reason) {
        log.warn("AUDIT: Transaction {} failed - Reason: {}", transactionId, reason);
    }

    /**
     * log dispute creation
     */

     public void logDisputeCreated(String disputeId, String transactionId) {
        log.info("AUDIT: Dispute {} created for transaction {}", disputeId, transactionId);
     } 

     /**
      * Log dispute resolution
      */
      public void logDisputeResolved(String disputed, String resolution) {
        log.info("AUDIT: Dispute {} resolved - Resolution: {}", disputed, resolution);
      }
}
