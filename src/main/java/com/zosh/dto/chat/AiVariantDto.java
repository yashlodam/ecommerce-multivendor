package com.zosh.dto.chat;

public class AiVariantDto {
    private Long id;
    private String variantName;
    private Integer mrpPrice;
    private Integer sellingPrice;
    private Integer discountPercent;
    private Integer quantity;
    private boolean inStock;
    private boolean isDefault;

    public AiVariantDto() {
    }

    public AiVariantDto(Long id, String variantName, Integer mrpPrice, Integer sellingPrice, Integer discountPercent, Integer quantity, boolean isDefault) {
        this.id = id;
        this.variantName = variantName;
        this.mrpPrice = mrpPrice;
        this.sellingPrice = sellingPrice;
        this.discountPercent = discountPercent;
        this.quantity = quantity != null ? quantity : 0;
        this.inStock = this.quantity > 0;
        this.isDefault = isDefault;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    public Integer getMrpPrice() {
        return mrpPrice;
    }

    public void setMrpPrice(Integer mrpPrice) {
        this.mrpPrice = mrpPrice;
    }

    public Integer getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(Integer sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public Integer getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(Integer discountPercent) {
        this.discountPercent = discountPercent;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        this.inStock = quantity != null && quantity > 0;
    }

    public boolean isInStock() {
        return inStock;
    }

    public void setInStock(boolean inStock) {
        this.inStock = inStock;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
}
