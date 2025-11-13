package com.carboncredit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carboncredit.entity.Notification;
import com.carboncredit.entity.User;
import com.carboncredit.repository.NotificationRepository;
import com.carboncredit.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * ✅ Generic notification method for any user - Saves to database
     * 
     * @param user The recipient user
     * @param title Notification title/subject
     * @param message Notification message body
     */
    public Notification notifyUser(User user, String title, String message) {
        return notifyUser(user, title, message, Notification.NotificationType.OTHER, null, null);
    }

    /**
     * ✅ Create notification with type and related entity
     */
    public Notification notifyUser(User user, String title, String message, 
                                   Notification.NotificationType type, 
                                   UUID relatedEntityId, 
                                   Notification.EntityType entityType) {
        if (user == null) {
            log.warn("Cannot notify: User is null");
            return null;
        }

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setNotificationType(type);
        notification.setRelatedEntityId(relatedEntityId);
        notification.setRelatedEntityType(entityType);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        Notification saved = notificationRepository.save(notification);
        
        log.info("📬 Notification created for user: {} | Title: {} | Type: {}", 
            user.getUsername(), title, type);

        return saved;
    }

    /**
     * Get all notifications for a user
     */
    public List<Notification> getUserNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Get unread notifications for a user
     */
    public List<Notification> getUnreadNotifications(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return List.of();
        return notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user);
    }

    /**
     * Get unread count for a user
     */
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    /**
     * Mark notification as read (with user verification)
     */
    public Notification markAsRead(UUID notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
        
        // Verify the notification belongs to the user
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to notification");
        }
        
        notification.markAsRead();
        Notification saved = notificationRepository.save(notification);
        log.info("Notification {} marked as read", notificationId);
        return saved;
    }

    /**
     * Mark notification as read (without user verification)
     */
    public void markAsRead(UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.markAsRead();
            notificationRepository.save(notification);
            log.info("Notification {} marked as read", notificationId);
        });
    }

    /**
     * Mark all notifications as read for a user (by User object)
     */
    public int markAllAsRead(User user) {
        return notificationRepository.markAllAsReadForUser(user);
    }

    /**
     * Mark all notifications as read for a user (by userId)
     */
    public int markAllAsRead(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return 0;
        return notificationRepository.markAllAsReadForUser(user);
    }

    /**
     * Delete notification (with user verification)
     */
    public void deleteNotification(UUID notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
        
        // Verify the notification belongs to the user
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to notification");
        }
        
        notificationRepository.delete(notification);
        log.info("Notification {} deleted by user {}", notificationId, user.getUsername());
    }

    /**
     * Delete notification (without user verification)
     */
    public void deleteNotification(UUID notificationId) {
        notificationRepository.deleteById(notificationId);
        log.info("Notification {} deleted", notificationId);
    }

    /**
     * Delete read notifications for a user
     */
    public int deleteReadNotifications(User user) {
        List<Notification> readNotifications = notificationRepository.findByUserOrderByCreatedAtDesc(user)
            .stream()
            .filter(Notification::getIsRead)
            .toList();
        
        notificationRepository.deleteAll(readNotifications);
        log.info("Deleted {} read notifications for user {}", readNotifications.size(), user.getUsername());
        return readNotifications.size();
    }

    /**
     * Get notification preferences (placeholder for future implementation)
     */
    public Map<String, Object> getNotificationPreferences(User user) {
        Map<String, Object> preferences = new HashMap<>();
        preferences.put("emailNotifications", true);
        preferences.put("pushNotifications", true);
        preferences.put("smsNotifications", false);
        preferences.put("userId", user.getId());
        preferences.put("username", user.getUsername());
        return preferences;
    }

    /**
     * Update notification preferences (placeholder for future implementation)
     */
    public Map<String, Object> updateNotificationPreferences(User user, Map<String, Boolean> preferences) {
        // TODO: Implement actual preference storage
        log.info("Updating notification preferences for user {}: {}", user.getUsername(), preferences);
        
        Map<String, Object> response = new HashMap<>();
        response.putAll(preferences);
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        response.put("updated", true);
        return response;
    }

    /**
     * Delete all notifications for a user
     */
    public void deleteAllNotifications(UUID userId) {
        List<Notification> notifications = getUserNotifications(userId);
        notificationRepository.deleteAll(notifications);
        log.info("All notifications deleted for user {}", userId);
    }

    /**
     * ✅ Notify multiple users with same message
     */
    public void notifyUsers(java.util.List<User> users, String title, String message) {
        users.forEach(user -> notifyUser(user, title, message));
    }

    // ========== Transaction Notifications ==========

    public void notifyTransactionInitiated(User buyer, User seller, UUID transactionId) {
        notifyUser(buyer, "💳 Giao dịch đã bắt đầu", 
            "Đơn mua của bạn đã được khởi tạo và đang chờ xử lý.",
            Notification.NotificationType.TRANSACTION_INITIATED, 
            transactionId, Notification.EntityType.TRANSACTION);
            
        notifyUser(seller, "💰 Đơn hàng mới", 
            "Có người mua đã khởi tạo giao dịch mua tín chỉ của bạn.",
            Notification.NotificationType.TRANSACTION_INITIATED, 
            transactionId, Notification.EntityType.TRANSACTION);
    }
    
    public void notifyTransactionCompleted(User buyer, User seller, UUID transactionId) {
        notifyUser(buyer, "✅ Mua hàng thành công", 
            "Giao dịch của bạn đã hoàn tất. Tín chỉ carbon đã được chuyển vào tài khoản.",
            Notification.NotificationType.TRANSACTION_COMPLETED, 
            transactionId, Notification.EntityType.TRANSACTION);
            
        notifyUser(seller, "💵 Bán hàng thành công", 
            "Giao dịch bán tín chỉ đã hoàn tất. Tiền đã được chuyển vào ví của bạn.",
            Notification.NotificationType.TRANSACTION_COMPLETED, 
            transactionId, Notification.EntityType.TRANSACTION);
    }

    public void notifyTransactionFailed(User buyer, User seller, UUID transactionId, String reason) {
        notifyUser(buyer, "❌ Giao dịch thất bại", 
            "Giao dịch của bạn không thể hoàn tất. Lý do: " + reason,
            Notification.NotificationType.TRANSACTION_FAILED, 
            transactionId, Notification.EntityType.TRANSACTION);
            
        notifyUser(seller, "❌ Giao dịch thất bại", 
            "Giao dịch bán tín chỉ thất bại. Lý do: " + reason,
            Notification.NotificationType.TRANSACTION_FAILED, 
            transactionId, Notification.EntityType.TRANSACTION);
    }

    // ========== Dispute Notifications ==========

    public void notifyDisputeCreated(User disputeRaiser, User otherParty, UUID disputeId) {
        notifyUser(disputeRaiser, "📋 Khiếu nại đã tạo", 
            "Khiếu nại của bạn đã được gửi và đang chờ xem xét.",
            Notification.NotificationType.DISPUTE_CREATED, 
            disputeId, Notification.EntityType.DISPUTE);
            
        notifyUser(otherParty, "⚠️ Thông báo khiếu nại", 
            "Có khiếu nại mới liên quan đến giao dịch của bạn. Vui lòng phản hồi.",
            Notification.NotificationType.DISPUTE_CREATED, 
            disputeId, Notification.EntityType.DISPUTE);
    }

    public void notifyDisputeResolved(User buyer, User seller, UUID disputeId, String resolution) {
        String message = "Khiếu nại đã được giải quyết. Kết quả: " + resolution;
        
        notifyUser(buyer, "✅ Khiếu nại đã giải quyết", message,
            Notification.NotificationType.DISPUTE_RESOLVED, 
            disputeId, Notification.EntityType.DISPUTE);
            
        notifyUser(seller, "✅ Khiếu nại đã giải quyết", message,
            Notification.NotificationType.DISPUTE_RESOLVED, 
            disputeId, Notification.EntityType.DISPUTE);
    }

    // ========== Credit/Journey Notifications ==========

    public void notifyCreditVerified(User evOwner, UUID creditId, Double amount) {
        notifyUser(evOwner, "🌱 Tín chỉ đã được phê duyệt", 
            String.format("Bạn đã kiếm được %.2f tCO₂ từ hành trình của mình.", amount),
            Notification.NotificationType.CREDIT_VERIFIED, 
            creditId, Notification.EntityType.CREDIT);
    }

    public void notifyCreditRejected(User evOwner, UUID creditId, String reason) {
        notifyUser(evOwner, "❌ Hành trình bị từ chối", 
            "Hành trình của bạn không được phê duyệt. Lý do: " + reason,
            Notification.NotificationType.CREDIT_REJECTED, 
            creditId, Notification.EntityType.CREDIT);
    }

    public void notifyCreditSold(User seller, UUID transactionId, Double amount, Double price) {
        notifyUser(seller, "💰 Bán tín chỉ thành công", 
            String.format("%.2f tCO₂ của bạn đã bán với giá %,.0f VND. Tiền đã chuyển vào ví.", amount, price),
            Notification.NotificationType.CREDIT_SOLD, 
            transactionId, Notification.EntityType.TRANSACTION);
    }

    // ========== Certificate Notifications ==========

    public void notifyCertificateIssued(User buyer, UUID certificateId) {
        notifyUser(buyer, "📜 Chứng chỉ đã được cấp", 
            "Chứng chỉ tiêu thụ carbon của bạn sẵn sàng tải xuống.",
            Notification.NotificationType.CERTIFICATE_ISSUED, 
            certificateId, Notification.EntityType.CERTIFICATE);
    }

    // ========== Wallet Notifications ==========

    public void notifyWalletUpdated(User user, String message) {
        notifyUser(user, "💰 Cập nhật ví", message,
            Notification.NotificationType.WALLET_UPDATED, 
            user.getId(), Notification.EntityType.WALLET);
    }

    // ========== System Notifications ==========

    public void notifySystemMaintenance(User user, String message) {
        notifyUser(user, "🔧 Bảo trì hệ thống", message,
            Notification.NotificationType.SYSTEM_MAINTENANCE, 
            null, null);
    }

    public void notifySecurityAlert(User user, String message) {
        notifyUser(user, "🛡️ Cảnh báo bảo mật", message,
            Notification.NotificationType.SECURITY_ALERT, 
            null, null);
    }
}
