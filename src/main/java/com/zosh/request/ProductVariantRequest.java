package com.zosh.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Request DTO for creating or updating a single product variant.
 * Used in both POST /sellers/products/{productId}/variants
 * and PUT /sellers/products/variants/{variantId}.
 */
public class ProductVariantRequest {

    /**
     * The label shown to the customer: "S", "128GB", "UK 8", etc.
     * For products with no real variants, pass "Standard".
     */
    @NotBlank(message = "Variant name is required (e.g. 'S', '128GB', 'Standard')")
    private String variantName;

    /**
     * Optional SKU — the seller's own stock code.
     * Leave blank to have the system generate one.
     */
    private String sku;

    @Positive(message = "MRP price must be positive")
    private int mrpPrice;

    @Positive(message = "Selling price must be positive")
    private int sellingPrice;

    @Min(value = 0, message = "Quantity cannot be negative")
    private int quantity;

    /**
     * True for the auto-generated "Standard" variant of products
     * that have no real size/storage axis.
     */
    private boolean isDefault = false;

    // ---- Getters and Setters ----

    public String getVariantName() { return variantName; }
    public void setVariantName(String variantName) { this.variantName = variantName; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public int getMrpPrice() { return mrpPrice; }
    public void setMrpPrice(int mrpPrice) { this.mrpPrice = mrpPrice; }

    public int getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(int sellingPrice) { this.sellingPrice = sellingPrice; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
}
