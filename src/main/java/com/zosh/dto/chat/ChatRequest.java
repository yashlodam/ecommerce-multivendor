package com.zosh.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChatRequest {

    private String sessionId;

    @NotBlank(message = "Message must not be blank")
    @Size(max = 2000, message = "Message must not exceed 2000 characters")
    private String message;

    public ChatRequest() {}

    public ChatRequest(String sessionId, String message) {
        this.sessionId = sessionId;
        this.message = message;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
