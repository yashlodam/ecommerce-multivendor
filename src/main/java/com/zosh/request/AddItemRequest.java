package com.zosh.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class AddItemRequest {

    /**
     * The variant ID selected by the customer.
     * When provided, the backend uses this variant's price and deducts its stock.
     * Null is accepted for backward compat (existing "Standard" variant products).
     */
    private Long variantId;

    /**
     * Fallback size string — used when variantId is null (legacy behavior).
     * Required only when no variantId is provided.
     */
    private String size;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity = 1;

    @NotNull(message = "Product ID is required")
    private Long productId;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
