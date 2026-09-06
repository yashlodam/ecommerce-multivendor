package com.zosh.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zosh.dto.chat.ChatRequest;
import com.zosh.dto.chat.ChatResponse;
import com.zosh.model.chat.ChatMessage;
import com.zosh.service.ai.ChatService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "AI Shopping Assistant", description = "ShopSphere AI-powered shopping assistant endpoints")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    @Operation(summary = "Send a message to the AI Shopping Assistant",
               description = "Send a natural language message and receive product recommendations, " +
                             "cart operations, order tracking, or store policy information. " +
                             "Include Authorization header for cart/order operations. " +
                             "Include sessionId in the request body to continue an existing conversation.")
    public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        ChatResponse response = chatService.chat(request, authHeader);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/{sessionId}")
    @Operation(summary = "Get conversation history",
               description = "Retrieve the full conversation history for a given session ID.")
    public ResponseEntity<List<ChatMessage>> getHistory(@PathVariable String sessionId) {
        List<ChatMessage> history = chatService.getHistory(sessionId);
        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/session/{sessionId}")
    @Operation(summary = "Delete a chat session",
               description = "Delete a chat session and all its messages.")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) {
        chatService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
