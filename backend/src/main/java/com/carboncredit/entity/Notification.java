package com.carboncredit.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue
    @Column(name = "notification_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Optional: Reference to related entities
    @Column(name = "related_entity_id")
    private UUID relatedEntityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "related_entity_type", length = 50)
    private EntityType relatedEntityType;

    // Enums for notification types and entity types
    public enum NotificationType {
        TRANSACTION_INITIATED,
        TRANSACTION_COMPLETED,
        TRANSACTION_FAILED,
        TRANSACTION_CANCELLED,
        PAYMENT_RECEIVED,
        PAYMENT_FAILED,
        CREDIT_VERIFIED,
        CREDIT_REJECTED,
        CREDIT_LISTED,
        CREDIT_SOLD,
        DISPUTE_CREATED,
        DISPUTE_RESOLVED,
        LISTING_EXPIRED,
        BID_RECEIVED,
        BID_OUTBID,
        AUCTION_WON,
        AUCTION_LOST,
        WALLET_UPDATED,
        CERTIFICATE_ISSUED,
        SYSTEM_MAINTENANCE,
        ACCOUNT_UPDATE,
        SECURITY_ALERT,
        WELCOME,
        OTHER
    }

    public enum EntityType {
        TRANSACTION,
        PAYMENT,
        CREDIT,
        LISTING,
        DISPUTE,
        CERTIFICATE,
        USER,
        WALLET
    }

    // Helper methods
    public boolean isUnread() {
        return !isRead;
    }

    public void markAsRead() {
        this.isRead = true;
    }

    public void markAsUnread() {
        this.isRead = false;
    }

    public boolean isTransactionRelated() {
        return notificationType == NotificationType.TRANSACTION_INITIATED ||
               notificationType == NotificationType.TRANSACTION_COMPLETED ||
               notificationType == NotificationType.TRANSACTION_FAILED ||
               notificationType == NotificationType.TRANSACTION_CANCELLED;
    }

    public boolean isPaymentRelated() {
        return notificationType == NotificationType.PAYMENT_RECEIVED ||
               notificationType == NotificationType.PAYMENT_FAILED;
    }

    public boolean isCreditRelated() {
        return notificationType == NotificationType.CREDIT_VERIFIED ||
               notificationType == NotificationType.CREDIT_REJECTED ||
               notificationType == NotificationType.CREDIT_LISTED ||
               notificationType == NotificationType.CREDIT_SOLD;
    }

    public boolean isDisputeRelated() {
        return notificationType == NotificationType.DISPUTE_CREATED ||
               notificationType == NotificationType.DISPUTE_RESOLVED;
    }

    public boolean isAuctionRelated() {
        return notificationType == NotificationType.BID_RECEIVED ||
               notificationType == NotificationType.BID_OUTBID ||
               notificationType == NotificationType.AUCTION_WON ||
               notificationType == NotificationType.AUCTION_LOST;
    }

    public boolean isSystemNotification() {
        return notificationType == NotificationType.SYSTEM_MAINTENANCE ||
               notificationType == NotificationType.SECURITY_ALERT;
    }

    // Static factory methods for common notifications
    public static Notification createTransactionNotification(User user, String title, String message, 
                                                           NotificationType type, UUID transactionId) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setNotificationType(type);
        notification.setRelatedEntityId(transactionId);
        notification.setRelatedEntityType(EntityType.TRANSACTION);
        notification.setIsRead(false);
        return notification;
    }

    public static Notification createPaymentNotification(User user, String title, String message, 
                                                        NotificationType type, UUID paymentId) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setNotificationType(type);
        notification.setRelatedEntityId(paymentId);
        notification.setRelatedEntityType(EntityType.PAYMENT);
        notification.setIsRead(false);
        return notification;
    }

    public static Notification createCreditNotification(User user, String title, String message, 
                                                       NotificationType type, UUID creditId) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setNotificationType(type);
        notification.setRelatedEntityId(creditId);
        notification.setRelatedEntityType(EntityType.CREDIT);
        notification.setIsRead(false);
        return notification;
    }

    public static Notification createSystemNotification(User user, String title, String message) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setNotificationType(NotificationType.SYSTEM_MAINTENANCE);
        notification.setIsRead(false);
        return notification;
    }

    @Override
    public String toString() {
        return String.format("Notification{id=%s, type=%s, title='%s', isRead=%s, userId=%s}", 
                           id, notificationType, title, isRead, user != null ? user.getId() : null);
    }
}
