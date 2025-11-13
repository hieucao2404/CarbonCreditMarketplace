package com.carboncredit.repository;

import com.carboncredit.entity.Notification;
import com.carboncredit.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // Find all notifications for a specific user
    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    // Find all notifications for a specific user with pagination
    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // Find unread notifications for a user
    List<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(User user);

    // Count unread notifications for a user
    long countByUserAndIsReadFalse(User user);

    // Find notifications by user and type
    List<Notification> findByUserAndNotificationTypeOrderByCreatedAtDesc(
        User user, Notification.NotificationType type);

    // Find recent notifications (within last N days)
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.createdAt >= :since ORDER BY n.createdAt DESC")
    List<Notification> findRecentNotifications(
        @Param("user") User user, 
        @Param("since") LocalDateTime since);

    // Mark all notifications as read for a user
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user = :user AND n.isRead = false")
    int markAllAsReadForUser(@Param("user") User user);

    // Delete old read notifications (cleanup)
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user = :user AND n.isRead = true AND n.createdAt < :before")
    int deleteOldReadNotifications(
        @Param("user") User user, 
        @Param("before") LocalDateTime before);

    // Find notifications by related entity
    List<Notification> findByRelatedEntityIdOrderByCreatedAtDesc(UUID relatedEntityId);

    // Find notifications by user ID (for when you have userId directly)
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);

    // Find unread count by user ID
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.isRead = false")
    long countUnreadByUserId(@Param("userId") UUID userId);
}
