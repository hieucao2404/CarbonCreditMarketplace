package com.carboncredit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.carboncredit.entity.User;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void notifyTransactionInitiated(User buyer, User seller, String listingId) {
        log.info("Notifying transaction initiated - Buyer: {}, Seller: {}, Listing: {}",
            buyer.getUsername(), seller.getUsername(), listingId);
    }
    
    public void notifyTransactionCompleted(User buyer, User seller, String transactionId) {
        log.info("Notifying transaction completed - Buyer: {}, Seller: {}, Transaction: {}",
            buyer.getUsername(), seller.getUsername(), transactionId);
    }

    public void notifyTransactionFailed(User buyer, User seller, String transactionId, String reason) {
        log.info("Notifying transaction failed - Buyer: {}, Seller: {}, Transaction: {}, Reason: {}",
            buyer.getUsername(), seller.getUsername(), transactionId, reason);
    }

    public void notifyDisputeCreated(User disputeRaiser, User otherParty, String transactionId) {
        log.info("Notifying dispute created - Raised by: {}, Other party: {}, Transaction: {}", 
            disputeRaiser.getUsername(), otherParty.getUsername(), transactionId);
    }

    public void notifyDisputeResolved(User buyer, User seller, String disputeId, String resolution) {
        log.info("Notifying dispute resolved - Buyer: {}, Seller: {}, Dispute: {}, Resolution: {}",
            buyer.getUsername(), seller.getUsername(), disputeId, resolution);
    }
}
