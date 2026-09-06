package com.zosh.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zosh.domain.NotificationType;
import com.zosh.domain.USER_ROLE;
import com.zosh.dto.notification.NotificationResponse;
import com.zosh.model.Notification;
import com.zosh.model.Seller;
import com.zosh.model.User;
import com.zosh.repository.NotificationRepository;
import com.zosh.repository.UserRepository;
import com.zosh.service.NotificationService;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    private void saveSafely(Notification notification) {
        try {
            notificationRepository.save(notification);
        } catch (Exception e) {
            log.error("Failed to persist notification for {}: {}", notification.getRecipientEmail(), e.getMessage(), e);
        }
    }

    @Override
    public void notifyUser(User user, NotificationType type, String title, String message, String referenceId, String referenceType, String actionUrl) {
        if (user == null || user.getEmail() == null) {
            log.warn("Attempted to notify null user or user without email");
            return;
        }
        Notification notif = new Notification();
        notif.setRecipientEmail(user.getEmail());
        notif.setRecipientRole(USER_ROLE.ROLE_CUSTOMER);
        notif.setUser(user);
        notif.setType(type);
        notif.setTitle(title);
        notif.setMessage(message);
        notif.setReferenceId(referenceId);
        notif.setReferenceType(referenceType);
        notif.setActionUrl(actionUrl);
        notif.setCreatedAt(LocalDateTime.now());
        saveSafely(notif);
    }

    @Override
    public void notifyUserByEmail(String email, USER_ROLE role, NotificationType type, String title, String message, String referenceId, String referenceType, String actionUrl) {
        if (email == null || email.trim().isEmpty()) {
            log.warn("Attempted to notify empty email");
            return;
        }
        Notification notif = new Notification();
        notif.setRecipientEmail(email);
        notif.setRecipientRole(role != null ? role : USER_ROLE.ROLE_CUSTOMER);
        notif.setType(type);
        notif.setTitle(title);
        notif.setMessage(message);
        notif.setReferenceId(referenceId);
        notif.setReferenceType(referenceType);
        notif.setActionUrl(actionUrl);
        notif.setCreatedAt(LocalDateTime.now());
        saveSafely(notif);
    }

    @Override
    public void notifySeller(Seller seller, NotificationType type, String title, String message, String referenceId, String referenceType, String actionUrl) {
        if (seller == null || seller.getEmail() == null) {
            log.warn("Attempted to notify null seller or seller without email");
            return;
        }
        Notification notif = new Notification();
        notif.setRecipientEmail(seller.getEmail());
        notif.setRecipientRole(USER_ROLE.ROLE_SELLER);
        notif.setSeller(seller);
        notif.setType(type);
        notif.setTitle(title);
        notif.setMessage(message);
        notif.setReferenceId(referenceId);
        notif.setReferenceType(referenceType);
        notif.setActionUrl(actionUrl);
        notif.setCreatedAt(LocalDateTime.now());
        saveSafely(notif);
    }

    @Override
    public void broadcastToAdmins(NotificationType type, String title, String message, String referenceId, String referenceType, String actionUrl) {
        try {
            List<User> admins = userRepository.findByRole(USER_ROLE.ROLE_ADMIN);
            if (admins != null && !admins.isEmpty()) {
                for (User admin : admins) {
                    Notification notif = new Notification();
                    notif.setRecipientEmail(admin.getEmail());
                    notif.setRecipientRole(USER_ROLE.ROLE_ADMIN);
                    notif.setUser(admin);
                    notif.setType(type);
                    notif.setTitle(title);
                    notif.setMessage(message);
                    notif.setReferenceId(referenceId);
                    notif.setReferenceType(referenceType);
                    notif.setActionUrl(actionUrl);
                    notif.setCreatedAt(LocalDateTime.now());
                    saveSafely(notif);
                }
            } else {
                log.debug("No active admins found to receive broadcast notification: {}", title);
            }
        } catch (Exception e) {
            log.error("Failed to broadcast notification to admins: {}", e.getMessage(), e);
        }
    }

    @Override
    public Page<NotificationResponse> getNotifications(String email, USER_ROLE role, Boolean unreadOnly, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        if (Boolean.TRUE.equals(unreadOnly)) {
            return notificationRepository
                .findByRecipientEmailAndRecipientRoleAndIsReadOrderByCreatedAtDesc(email, role, false, pageable)
                .map(NotificationResponse::fromEntity);
        } else {
            return notificationRepository
                .findByRecipientEmailAndRecipientRoleOrderByCreatedAtDesc(email, role, pageable)
                .map(NotificationResponse::fromEntity);
        }
    }

    @Override
    public long getUnreadCount(String email, USER_ROLE role) {
        return notificationRepository.countByRecipientEmailAndRecipientRoleAndIsReadFalse(email, role);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, String email) {
        Notification notif = notificationRepository.findByIdAndRecipientEmail(notificationId, email)
            .orElseThrow(() -> new RuntimeException("Notification not found or unauthorized"));
        if (!notif.isRead()) {
            notif.setRead(true);
            notif.setReadAt(LocalDateTime.now());
            notif = notificationRepository.save(notif);
        }
        return NotificationResponse.fromEntity(notif);
    }

    @Override
    @Transactional
    public int markAllAsRead(String email, USER_ROLE role) {
        return notificationRepository.markAllAsRead(email, role, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId, String email) {
        Notification notif = notificationRepository.findByIdAndRecipientEmail(notificationId, email)
            .orElseThrow(() -> new RuntimeException("Notification not found or unauthorized"));
        notificationRepository.delete(notif);
    }

    @Override
    @Transactional
    public int deleteAllRead(String email, USER_ROLE role) {
        return notificationRepository.deleteAllRead(email, role);
    }
}
