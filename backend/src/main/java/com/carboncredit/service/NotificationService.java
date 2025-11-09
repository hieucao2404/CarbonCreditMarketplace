package com.carboncredit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.carboncredit.entity.User;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    /**
     * ✅ Generic notification method for any user
     * 
     * Used by:
     * - VerificationService: notifying EV owners and CVAs about inspection status
     * - TransactionService: notifying parties about transaction updates
     * - DisputeService: notifying users about dispute updates
     * - WalletService: notifying users about wallet transactions
     * 
     * @param user The recipient user
     * @param title Notification title/subject
     * @param message Notification message body
     */
    public void notifyUser(User user, String title, String message) {
        if (user == null) {
            log.warn("Cannot notify: User is null");
            return;
        }

        log.info("📬 Notifying user: {} | Title: {} | Message: {}",
            user.getUsername(), title, message);

        // TODO: Implement actual notification delivery
        // Possible implementations:
        // 1. Save to database (Notification entity)
        // 2. Send email
        // 3. Send SMS
        // 4. Push notification to mobile app
        // 5. WebSocket for real-time in-app notification
        
        // Example placeholder for future implementation:
        // notificationRepository.save(new Notification(user, title, message, LocalDateTime.now()));
        // emailService.sendEmail(user.getEmail(), title, message);
        // webSocketService.sendNotification(user.getId(), title, message);
    }

    /**
     * ✅ Notify multiple users with same message
     */
    public void notifyUsers(java.util.List<User> users, String title, String message) {
        users.forEach(user -> notifyUser(user, title, message));
    }

    public void notifyTransactionInitiated(User buyer, User seller, String listingId) {
        log.info("Notifying transaction initiated - Buyer: {}, Seller: {}, Listing: {}",
            buyer.getUsername(), seller.getUsername(), listingId);
        
        // Notify both parties
        notifyUser(buyer, "Transaction Initiated", 
            "Your purchase order for listing " + listingId + " has been initiated.");
        notifyUser(seller, "New Purchase Order", 
            "A buyer has initiated a purchase for your listing " + listingId + ".");
    }
    
    public void notifyTransactionCompleted(User buyer, User seller, String transactionId) {
        log.info("Notifying transaction completed - Buyer: {}, Seller: {}, Transaction: {}",
            buyer.getUsername(), seller.getUsername(), transactionId);
        
        notifyUser(buyer, "Purchase Completed", 
            "Your purchase transaction " + transactionId + " has been completed successfully.");
        notifyUser(seller, "Sale Completed", 
            "Your sale transaction " + transactionId + " has been completed successfully.");
    }

    public void notifyTransactionFailed(User buyer, User seller, String transactionId, String reason) {
        log.info("Notifying transaction failed - Buyer: {}, Seller: {}, Transaction: {}, Reason: {}",
            buyer.getUsername(), seller.getUsername(), transactionId, reason);
        
        notifyUser(buyer, "Transaction Failed", 
            "Your transaction " + transactionId + " failed. Reason: " + reason);
        notifyUser(seller, "Transaction Failed", 
            "Transaction " + transactionId + " failed. Reason: " + reason);
    }

    public void notifyDisputeCreated(User disputeRaiser, User otherParty, String transactionId) {
        log.info("Notifying dispute created - Raised by: {}, Other party: {}, Transaction: {}", 
            disputeRaiser.getUsername(), otherParty.getUsername(), transactionId);
        
        notifyUser(disputeRaiser, "Dispute Created", 
            "Your dispute for transaction " + transactionId + " has been submitted.");
        notifyUser(otherParty, "Dispute Notification", 
            "A dispute has been raised for transaction " + transactionId + ". Please respond.");
    }

    public void notifyDisputeResolved(User buyer, User seller, String disputeId, String resolution) {
        log.info("Notifying dispute resolved - Buyer: {}, Seller: {}, Dispute: {}, Resolution: {}",
            buyer.getUsername(), seller.getUsername(), disputeId, resolution);
        
        notifyUser(buyer, "Dispute Resolved", 
            "Dispute " + disputeId + " has been resolved. Resolution: " + resolution);
        notifyUser(seller, "Dispute Resolved", 
            "Dispute " + disputeId + " has been resolved. Resolution: " + resolution);
    }
}
