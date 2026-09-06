package com.zosh.service.ai;

import java.util.List;

import com.zosh.dto.chat.ChatRequest;
import com.zosh.dto.chat.ChatResponse;
import com.zosh.model.chat.ChatMessage;

public interface ChatService {

    ChatResponse chat(ChatRequest request, String authHeader);

    List<ChatMessage> getHistory(String sessionId);

    void deleteSession(String sessionId);
}
