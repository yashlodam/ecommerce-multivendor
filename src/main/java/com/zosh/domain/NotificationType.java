package com.zosh.domain;

/**
 * Defines domain-specific, actionable notification types across
 * Customer, Seller, and Admin roles in ShopSphere.
 */
public enum NotificationType {

    // ─── Customer Notifications ───────────────────────────────────────────────
    ORDER_PLACED,
    ORDER_CONFIRMED,
    ORDER_SHIPPED,
    ORDER_DELIVERED,
    ORDER_CANCELLED,
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    DEAL_STARTED,

    // ─── Seller Notifications ─────────────────────────────────────────────────
    NEW_ORDER,
    ORDER_CANCELLED_SELLER,
    LOW_STOCK,
    SELLER_ACCOUNT_UPDATE,
    DEAL_CREATED_SELLER,

    // ─── Admin Notifications ──────────────────────────────────────────────────
    NEW_SELLER,
    SELLER_APPROVAL_REQUIRED,
    NEW_ORDER_ADMIN,
    PAYMENT_ISSUE,
    SYSTEM_ALERT
}
