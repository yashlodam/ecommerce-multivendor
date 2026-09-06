package com.zosh.dto.notification;

import java.time.LocalDateTime;

import com.zosh.domain.NotificationType;
import com.zosh.model.Notification;

/**
 * Client-safe representation of a persistent notification.
 * Never exposes internal JPA entity graphs directly.
 */
public class NotificationResponse {

    private Long id;
    private NotificationType type;
    private String title;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private String referenceId;
    private String referenceType;
    private String actionUrl;

    public NotificationResponse() {
    }

    public static NotificationResponse fromEntity(Notification entity) {
        if (entity == null) {
            return null;
        }
        NotificationResponse dto = new NotificationResponse();
        dto.setId(entity.getId());
        dto.setType(entity.getType());
        dto.setTitle(entity.getTitle());
        dto.setMessage(entity.getMessage());
        dto.setRead(entity.isRead());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setReadAt(entity.getReadAt());
        dto.setReferenceId(entity.getReferenceId());
        dto.setReferenceType(entity.getReferenceType());
        dto.setActionUrl(entity.getActionUrl());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }
}
