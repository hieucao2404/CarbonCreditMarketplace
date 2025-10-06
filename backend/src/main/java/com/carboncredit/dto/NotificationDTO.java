package com.carboncredit.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.carboncredit.entity.Notification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private UUID id;
    private UUID userId;
    private String username;
    private String notificationType;
    private String title;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private UUID relatedEntityId;
    private String relatedEntityType;

    // Constructor from Notification entity
    public NotificationDTO(Notification notification) {
        this.id = notification.getId();
        this.userId = notification.getUser() != null ? notification.getUser().getId() : null;
        this.username = notification.getUser() != null ? notification.getUser().getUsername() : null;
        this.notificationType = notification.getNotificationType() != null ? notification.getNotificationType().toString() : null;
        this.title = notification.getTitle();
        this.message = notification.getMessage();
        this.isRead = notification.getIsRead();
        this.createdAt = notification.getCreatedAt();
        this.relatedEntityId = notification.getRelatedEntityId();
        this.relatedEntityType = notification.getRelatedEntityType() != null ? notification.getRelatedEntityType().toString() : null;
    }
}