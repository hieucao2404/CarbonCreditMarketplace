package com.carboncredit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

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
}
