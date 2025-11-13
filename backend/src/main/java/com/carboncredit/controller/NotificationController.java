package com.carboncredit.controller;

import com.carboncredit.dto.ApiResponse;
import com.carboncredit.entity.Notification;
import com.carboncredit.entity.User;
import com.carboncredit.service.NotificationService;
import com.carboncredit.service.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    /**
     * Get all notifications for current user
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getNotifications(Authentication auth) {
        try {
            String username = auth.getName();
            
            // ✅ FIXED: Handle Optional<User> properly
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            log.info("📬 Fetching notifications for user: {}", username);

            List<Notification> notifications = notificationService.getUserNotifications(user.getId());
            
            // Transform notifications to response format
            List<Map<String, Object>> response = notifications.stream()
                    .map(this::mapNotificationToResponse)
                    .collect(Collectors.toList());

            log.info("✅ Retrieved {} notifications for user: {}", response.size(), username);
            
            return ResponseEntity.ok(new ApiResponse<>(true, 
                "Notifications retrieved successfully", response));
                
        } catch (Exception e) {
            log.error("❌ Error fetching notifications", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Error fetching notifications: " + e.getMessage(), null));
        }
    }

    /**
     * Get unread notifications count
     */
    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUnreadCount(Authentication auth) {
        try {
            String username = auth.getName();
            
            // ✅ FIXED: Handle Optional<User> properly
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            long unreadCount = notificationService.getUnreadCount(user.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("unreadCount", unreadCount);
            response.put("username", username);
            response.put("timestamp", LocalDateTime.now());

            log.info("📊 Unread count for {}: {}", username, unreadCount);
            
            return ResponseEntity.ok(new ApiResponse<>(true, "Unread count retrieved", response));
            
        } catch (Exception e) {
            log.error("❌ Error fetching unread count", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Error: " + e.getMessage(), null));
        }
    }

    /**
     * Mark notification as read
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAsRead(
            @PathVariable UUID notificationId,
            Authentication auth) {
        try {
            String username = auth.getName();
            
            // ✅ FIXED: Handle Optional<User> properly
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            log.info("📧 User {} marking notification {} as read", username, notificationId);

            Notification notification = notificationService.markAsRead(notificationId, user);
            
            Map<String, Object> response = mapNotificationToResponse(notification);

            log.info("✅ Notification {} marked as read", notificationId);
            
            return ResponseEntity.ok(new ApiResponse<>(true, "Notification marked as read", response));
            
        } catch (Exception e) {
            log.error("❌ Error marking notification as read", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Error: " + e.getMessage(), null));
        }
    }

    /**
     * Mark all notifications as read
     */
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAllAsRead(Authentication auth) {
        try {
            String username = auth.getName();
            
            // ✅ FIXED: Handle Optional<User> properly
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            log.info("📧 User {} marking all notifications as read", username);

            int updatedCount = notificationService.markAllAsRead(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("updatedCount", updatedCount);
            response.put("username", username);
            response.put("timestamp", LocalDateTime.now());

            log.info("✅ Marked {} notifications as read for {}", updatedCount, username);
            
            return ResponseEntity.ok(new ApiResponse<>(true, 
                updatedCount + " notifications marked as read", response));
                
        } catch (Exception e) {
            log.error("❌ Error marking all notifications as read", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Error: " + e.getMessage(), null));
        }
    }

    /**
     * Delete notification
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @PathVariable UUID notificationId,
            Authentication auth) {
        try {
            String username = auth.getName();
            
            // ✅ FIXED: Handle Optional<User> properly
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            log.info("🗑️ User {} deleting notification {}", username, notificationId);

            notificationService.deleteNotification(notificationId, user);

            log.info("✅ Notification {} deleted", notificationId);
            
            return ResponseEntity.ok(new ApiResponse<>(true, "Notification deleted successfully", null));
            
        } catch (Exception e) {
            log.error("❌ Error deleting notification", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Error: " + e.getMessage(), null));
        }
    }

    /**
     * Clear all read notifications
     */
    @DeleteMapping("/clear-read")
    public ResponseEntity<ApiResponse<Map<String, Object>>> clearReadNotifications(Authentication auth) {
        try {
            String username = auth.getName();
            
            // ✅ FIXED: Handle Optional<User> properly
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            log.info("🗑️ User {} clearing all read notifications", username);

            int deletedCount = notificationService.deleteReadNotifications(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("deletedCount", deletedCount);
            response.put("username", username);
            response.put("timestamp", LocalDateTime.now());

            log.info("✅ Cleared {} read notifications for {}", deletedCount, username);
            
            return ResponseEntity.ok(new ApiResponse<>(true, 
                deletedCount + " read notifications cleared", response));
                
        } catch (Exception e) {
            log.error("❌ Error clearing read notifications", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Error: " + e.getMessage(), null));
        }
    }

    /**
     * Get notification preferences
     */
    @GetMapping("/preferences")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNotificationPreferences(Authentication auth) {
        try {
            String username = auth.getName();
            
            // ✅ FIXED: Handle Optional<User> properly
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            Map<String, Object> preferences = notificationService.getNotificationPreferences(user);

            log.info("⚙️ Retrieved notification preferences for {}", username);
            
            return ResponseEntity.ok(new ApiResponse<>(true, 
                "Preferences retrieved successfully", preferences));
                
        } catch (Exception e) {
            log.error("❌ Error fetching notification preferences", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Error: " + e.getMessage(), null));
        }
    }

    /**
     * Update notification preferences
     */
    @PutMapping("/preferences")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateNotificationPreferences(
            @RequestBody Map<String, Boolean> preferences,
            Authentication auth) {
        try {
            String username = auth.getName();
            
            // ✅ FIXED: Handle Optional<User> properly
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            log.info("⚙️ User {} updating notification preferences", username);

            Map<String, Object> updatedPreferences = notificationService.updateNotificationPreferences(
                user, preferences
            );

            log.info("✅ Notification preferences updated for {}", username);
            
            return ResponseEntity.ok(new ApiResponse<>(true, 
                "Preferences updated successfully", updatedPreferences));
                
        } catch (Exception e) {
            log.error("❌ Error updating notification preferences", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Error: " + e.getMessage(), null));
        }
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    /**
     * Map Notification entity to response format
     * 
     * @param notification Notification entity
     * @return Map with formatted notification data
     */
    private Map<String, Object> mapNotificationToResponse(Notification notification) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", notification.getId());
        map.put("type", notification.getNotificationType().toString());
        map.put("title", notification.getTitle());
        map.put("message", notification.getMessage());
        map.put("read", notification.getIsRead());
        map.put("createdAt", notification.getCreatedAt());
        
        // Add relative time (e.g., "5 phút trước")
        map.put("time", getTimeAgo(notification.getCreatedAt()));
        
        // Add related entity info if available
        if (notification.getRelatedEntityId() != null) {
            map.put("relatedEntityId", notification.getRelatedEntityId().toString());
        }
        
        if (notification.getRelatedEntityType() != null) {
            map.put("relatedEntityType", notification.getRelatedEntityType().toString());
        }
        
        return map;
    }

    /**
     * Calculate relative time ago (e.g., "5 minutes ago")
     * 
     * @param dateTime DateTime to calculate from
     * @return Human-readable relative time string
     */
    private String getTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "Không rõ";
        }

        LocalDateTime now = LocalDateTime.now();
        
        long minutes = ChronoUnit.MINUTES.between(dateTime, now);
        if (minutes < 1) {
            return "Vừa xong";
        }
        if (minutes < 60) {
            return minutes + " phút trước";
        }

        long hours = ChronoUnit.HOURS.between(dateTime, now);
        if (hours < 24) {
            return hours + " giờ trước";
        }

        long days = ChronoUnit.DAYS.between(dateTime, now);
        if (days == 1) {
            return "Hôm qua";
        }
        if (days < 7) {
            return days + " ngày trước";
        }

        long weeks = ChronoUnit.WEEKS.between(dateTime, now);
        if (weeks < 4) {
            return weeks + " tuần trước";
        }

        long months = ChronoUnit.MONTHS.between(dateTime, now);
        if (months < 12) {
            return months + " tháng trước";
        }

        long years = ChronoUnit.YEARS.between(dateTime, now);
        return years + " năm trước";
    }
}
