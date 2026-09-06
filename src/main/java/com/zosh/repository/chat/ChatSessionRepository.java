package com.zosh.repository.chat;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.zosh.model.chat.ChatSession;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    Optional<ChatSession> findBySessionToken(String sessionToken);

    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId);

    @Query("SELECT s FROM ChatSession s LEFT JOIN FETCH s.messages WHERE s.sessionToken = :sessionToken")
    Optional<ChatSession> findBySessionTokenWithMessages(@Param("sessionToken") String sessionToken);
}
