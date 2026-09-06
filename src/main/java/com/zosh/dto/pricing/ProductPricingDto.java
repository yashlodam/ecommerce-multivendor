package com.zosh.dto.pricing;

import java.time.LocalDateTime;

/**
 * Encapsulates the pricing breakdown of a product or variant after applying deals.
 */
public class ProductPricingDto {

    private Long productId;
    private Long variantId;
    private Integer basePrice;
    private Integer mrpPrice;
    private Integer effectivePrice;
    private Integer discountAmount;
    private Integer discountPercentage;
    private boolean dealActive;
    private Long appliedDealId;
    private String appliedDealTitle;
    private String dealType;
    private LocalDateTime dealEndsAt;

    public ProductPricingDto() {
    }

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

    public Integer getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(Integer basePrice) {
        this.basePrice = basePrice;
    }

    public Integer getMrpPrice() {
        return mrpPrice;
    }

    public void setMrpPrice(Integer mrpPrice) {
        this.mrpPrice = mrpPrice;
    }

    public Integer getEffectivePrice() {
        return effectivePrice;
    }

    public void setEffectivePrice(Integer effectivePrice) {
        this.effectivePrice = effectivePrice;
    }

    public Integer getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Integer discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Integer getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(Integer discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public boolean isDealActive() {
        return dealActive;
    }

    public void setDealActive(boolean dealActive) {
        this.dealActive = dealActive;
    }

    public Long getAppliedDealId() {
        return appliedDealId;
    }

    public void setAppliedDealId(Long appliedDealId) {
        this.appliedDealId = appliedDealId;
    }

    public String getAppliedDealTitle() {
        return appliedDealTitle;
    }

    public void setAppliedDealTitle(String appliedDealTitle) {
        this.appliedDealTitle = appliedDealTitle;
    }

    public String getDealType() {
        return dealType;
    }

    public void setDealType(String dealType) {
        this.dealType = dealType;
    }

    public LocalDateTime getDealEndsAt() {
        return dealEndsAt;
    }

    public void setDealEndsAt(LocalDateTime dealEndsAt) {
        this.dealEndsAt = dealEndsAt;
    }
}
