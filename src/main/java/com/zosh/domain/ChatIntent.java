package com.zosh.domain;

public enum ChatIntent {
    // Conversational & Meta Intents
    GREETING,
    GENERAL_CONVERSATION,
    GRATITUDE,
    FAREWELL,
    HELP,
    OFF_TOPIC,

    // Product Intents
    PRODUCT_SEARCH,
    PRODUCT_RECOMMENDATION,
    PRODUCT_DETAILS,
    PRODUCT_COMPARISON,
    VARIANT_AVAILABILITY,
    SIMILAR_PRODUCTS,

    // Cart Intents
    CART_VIEW,
    CART_ADD,
    CART_UPDATE,
    CART_REMOVE,
    CART_CLEAR,

    // Order Intents
    ORDER_TRACKING,
    ORDER_HISTORY,
    ORDER_CANCEL,

    // Store, Policy, Seller & Review Intents
    STORE_POLICY,
    PAYMENT_INFO,
    SHIPPING_INFO,
    RETURN_POLICY,
    FAQ,
    SELLER_INFO,
    REVIEW_INFO,

    GENERAL_QUERY,
    UNKNOWN
}
