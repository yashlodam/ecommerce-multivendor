package com.zosh.dto.chat;

import java.util.ArrayList;
import java.util.List;

import com.zosh.domain.ChatIntent;

public class ChatResponse {
    private String sessionId;
    private String message;
    private ChatIntent intent;
    private List<AiProductDto> products = new ArrayList<>();
    private List<AiActionDto> actions = new ArrayList<>();
    private AiCartSummaryDto cartSummary;
    private AiOrderSummaryDto orderSummary;
    private String executionMode;

    public ChatResponse() {
    }

    public ChatResponse(String sessionId, String message, ChatIntent intent) {
        this.sessionId = sessionId;
        this.message = message;
        this.intent = intent;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ChatIntent getIntent() {
        return intent;
    }

    public void setIntent(ChatIntent intent) {
        this.intent = intent;
    }

    public List<AiProductDto> getProducts() {
        return products;
    }

    public void setProducts(List<AiProductDto> products) {
        this.products = products != null ? products : new ArrayList<>();
    }

    public List<AiActionDto> getActions() {
        return actions;
    }

    public void setActions(List<AiActionDto> actions) {
        this.actions = actions != null ? actions : new ArrayList<>();
    }

    public AiCartSummaryDto getCartSummary() {
        return cartSummary;
    }

    public void setCartSummary(AiCartSummaryDto cartSummary) {
        this.cartSummary = cartSummary;
    }

    public AiOrderSummaryDto getOrderSummary() {
        return orderSummary;
    }

    public void setOrderSummary(AiOrderSummaryDto orderSummary) {
        this.orderSummary = orderSummary;
    }

    public String getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(String executionMode) {
        this.executionMode = executionMode;
    }
}
