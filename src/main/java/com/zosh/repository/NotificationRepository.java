package com.zosh.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.zosh.domain.USER_ROLE;
import com.zosh.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientEmailAndRecipientRoleOrderByCreatedAtDesc(
        String recipientEmail,
        USER_ROLE recipientRole,
        Pageable pageable
    );

    Page<Notification> findByRecipientEmailAndRecipientRoleAndIsReadOrderByCreatedAtDesc(
        String recipientEmail,
        USER_ROLE recipientRole,
        boolean isRead,
        Pageable pageable
    );

    long countByRecipientEmailAndRecipientRoleAndIsReadFalse(
        String recipientEmail,
        USER_ROLE recipientRole
    );

    Optional<Notification> findByIdAndRecipientEmail(Long id, String recipientEmail);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :now WHERE n.recipientEmail = :recipientEmail AND n.recipientRole = :recipientRole AND n.isRead = false")
    int markAllAsRead(
        @Param("recipientEmail") String recipientEmail,
        @Param("recipientRole") USER_ROLE recipientRole,
        @Param("now") LocalDateTime now
    );

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.recipientEmail = :recipientEmail AND n.recipientRole = :recipientRole AND n.isRead = true")
    int deleteAllRead(
        @Param("recipientEmail") String recipientEmail,
        @Param("recipientRole") USER_ROLE recipientRole
    );

    @Modifying
    @Query("UPDATE Notification n SET n.user = null WHERE n.user.id = :userId")
    void unlinkUserFromNotifications(@Param("userId") Long userId);
}
