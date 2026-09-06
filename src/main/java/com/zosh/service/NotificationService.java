package com.zosh.service;

import org.springframework.data.domain.Page;

import com.zosh.domain.NotificationType;
import com.zosh.domain.USER_ROLE;
import com.zosh.dto.notification.NotificationResponse;
import com.zosh.model.Seller;
import com.zosh.model.User;

public interface NotificationService {

    void notifyUser(User user, NotificationType type, String title, String message, String referenceId, String referenceType, String actionUrl);

    void notifyUserByEmail(String email, USER_ROLE role, NotificationType type, String title, String message, String referenceId, String referenceType, String actionUrl);

    void notifySeller(Seller seller, NotificationType type, String title, String message, String referenceId, String referenceType, String actionUrl);

    void broadcastToAdmins(NotificationType type, String title, String message, String referenceId, String referenceType, String actionUrl);

    Page<NotificationResponse> getNotifications(String email, USER_ROLE role, Boolean unreadOnly, int page, int size);

    long getUnreadCount(String email, USER_ROLE role);

    NotificationResponse markAsRead(Long notificationId, String email);

    int markAllAsRead(String email, USER_ROLE role);

    void deleteNotification(Long notificationId, String email);

    int deleteAllRead(String email, USER_ROLE role);
}
