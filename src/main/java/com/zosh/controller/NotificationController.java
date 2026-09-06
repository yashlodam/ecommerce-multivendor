package com.zosh.controller;

import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zosh.domain.USER_ROLE;
import com.zosh.dto.notification.NotificationResponse;
import com.zosh.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Persistent notification management for Customers, Sellers, and Admins")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private NotificationService notificationService;

    private USER_ROLE resolveRole(Authentication auth, USER_ROLE requestedRole) {
        boolean isAdmin = false;
        boolean isSeller = false;
        boolean isCustomer = false;

        if (auth != null && auth.getAuthorities() != null) {
            for (GrantedAuthority ga : auth.getAuthorities()) {
                String a = ga.getAuthority();
                if ("ROLE_ADMIN".equals(a)) isAdmin = true;
                if ("ROLE_SELLER".equals(a)) isSeller = true;
                if ("ROLE_CUSTOMER".equals(a)) isCustomer = true;
            }
        }

        if (requestedRole != null) {
            if (requestedRole == USER_ROLE.ROLE_ADMIN && isAdmin) return USER_ROLE.ROLE_ADMIN;
            if (requestedRole == USER_ROLE.ROLE_SELLER && isSeller) return USER_ROLE.ROLE_SELLER;
            if (requestedRole == USER_ROLE.ROLE_CUSTOMER && (isCustomer || isSeller || isAdmin)) return USER_ROLE.ROLE_CUSTOMER;
        }

        if (isAdmin) return USER_ROLE.ROLE_ADMIN;
        if (isSeller) return USER_ROLE.ROLE_SELLER;
        return USER_ROLE.ROLE_CUSTOMER;
    }

    @GetMapping
    @Operation(summary = "Get paginated notifications for authenticated user/seller/admin")
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(required = false) USER_ROLE role) {

        String email = auth.getName();
        USER_ROLE targetRole = resolveRole(auth, role);

        Page<NotificationResponse> notifications = notificationService.getNotifications(email, targetRole, unreadOnly, page, size);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notifications count for authenticated user")
    public ResponseEntity<Map<String, Object>> getUnreadCount(
            Authentication auth,
            @RequestParam(required = false) USER_ROLE role) {

        String email = auth.getName();
        USER_ROLE targetRole = resolveRole(auth, role);

        long count = notificationService.getUnreadCount(email, targetRole);
        return ResponseEntity.ok(Collections.singletonMap("count", count));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a single notification as read")
    public ResponseEntity<NotificationResponse> markAsRead(
            Authentication auth,
            @PathVariable Long id) {

        String email = auth.getName();
        NotificationResponse response = notificationService.markAsRead(id, email);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read for authenticated user/role")
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            Authentication auth,
            @RequestParam(required = false) USER_ROLE role) {

        String email = auth.getName();
        USER_ROLE targetRole = resolveRole(auth, role);

        int updated = notificationService.markAllAsRead(email, targetRole);
        return ResponseEntity.ok(Collections.singletonMap("updated", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a single notification")
    public ResponseEntity<Map<String, Object>> deleteNotification(
            Authentication auth,
            @PathVariable Long id) {

        String email = auth.getName();
        notificationService.deleteNotification(id, email);
        return ResponseEntity.ok(Collections.singletonMap("message", "Notification deleted successfully"));
    }

    @DeleteMapping("/read")
    @Operation(summary = "Delete all read notifications for authenticated user/role")
    public ResponseEntity<Map<String, Object>> deleteAllRead(
            Authentication auth,
            @RequestParam(required = false) USER_ROLE role) {

        String email = auth.getName();
        USER_ROLE targetRole = resolveRole(auth, role);

        int deleted = notificationService.deleteAllRead(email, targetRole);
        return ResponseEntity.ok(Collections.singletonMap("deleted", deleted));
    }
}
