package com.zosh.model.chat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.zosh.model.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "chat_sessions",
    indexes = {
        @Index(name = "idx_chat_session_token", columnList = "sessionToken", unique = true),
        @Index(name = "idx_chat_session_user_id", columnList = "user_id")
    }
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chat_session_seq")
    @SequenceGenerator(name = "chat_session_seq", sequenceName = "chat_session_sequence", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String sessionToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"addresses", "usedCoupons", "password", "hibernateLazyInitializer", "handler"})
    private User user;

    private String lastIntent;
    private Long lastReferencedProductId;
    private Long lastReferencedVariantId;

    private String lastSearchQuery;
    private String lastCategory;
    private Integer lastMinPrice;
    private Integer lastMaxPrice;
    private String lastColor;
    private String lastSize;
    private String lastBrand;

    @Column(columnDefinition = "TEXT")
    private String lastProductIdsJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ChatMessage> messages = new ArrayList<>();

    public ChatSession() {}

    public ChatSession(String sessionToken, User user) {
        this.sessionToken = sessionToken;
        this.user = user;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getLastIntent() { return lastIntent; }
    public void setLastIntent(String lastIntent) { this.lastIntent = lastIntent; }
    public Long getLastReferencedProductId() { return lastReferencedProductId; }
    public void setLastReferencedProductId(Long lastReferencedProductId) { this.lastReferencedProductId = lastReferencedProductId; }
    public Long getLastReferencedVariantId() { return lastReferencedVariantId; }
    public void setLastReferencedVariantId(Long lastReferencedVariantId) { this.lastReferencedVariantId = lastReferencedVariantId; }

    public String getLastSearchQuery() { return lastSearchQuery; }
    public void setLastSearchQuery(String lastSearchQuery) { this.lastSearchQuery = lastSearchQuery; }
    public String getLastCategory() { return lastCategory; }
    public void setLastCategory(String lastCategory) { this.lastCategory = lastCategory; }
    public Integer getLastMinPrice() { return lastMinPrice; }
    public void setLastMinPrice(Integer lastMinPrice) { this.lastMinPrice = lastMinPrice; }
    public Integer getLastMaxPrice() { return lastMaxPrice; }
    public void setLastMaxPrice(Integer lastMaxPrice) { this.lastMaxPrice = lastMaxPrice; }
    public String getLastColor() { return lastColor; }
    public void setLastColor(String lastColor) { this.lastColor = lastColor; }
    public String getLastSize() { return lastSize; }
    public void setLastSize(String lastSize) { this.lastSize = lastSize; }
    public String getLastBrand() { return lastBrand; }
    public void setLastBrand(String lastBrand) { this.lastBrand = lastBrand; }
    public String getLastProductIdsJson() { return lastProductIdsJson; }
    public void setLastProductIdsJson(String lastProductIdsJson) { this.lastProductIdsJson = lastProductIdsJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<ChatMessage> getMessages() { return messages; }
    public void setMessages(List<ChatMessage> messages) { this.messages = messages; }

    public void addMessage(ChatMessage message) {
        this.messages.add(message);
        message.setSession(this);
    }
}
