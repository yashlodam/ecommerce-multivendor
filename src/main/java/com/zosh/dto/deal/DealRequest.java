package com.zosh.dto.deal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.zosh.domain.DealType;
import com.zosh.domain.DiscountType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DealRequest {

    private String title;

    private String description;

    private DealType dealType;

    private DiscountType discountType = DiscountType.PERCENTAGE;

    private BigDecimal discountValue;

    /** Legacy discount integer percentage for backward compatibility */
    private Integer discount;

    private BigDecimal maxDiscountAmount;

    private BigDecimal minOrderAmount;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private Boolean active = true;

    private Integer usageLimit;

    /** Product IDs for PRODUCT-level deals */
    private List<Long> productIds = new ArrayList<>();

    /** Category slug for CATEGORY-level deals */
    private String categorySlug;

    /** Category ID (for legacy compatibility) */
    private Long categoryId;

    /** Target seller ID (only configurable by Admin) */
    private Long sellerId;

    public DealRequest() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DealType getDealType() {
        return dealType;
    }

    public void setDealType(DealType dealType) {
        this.dealType = dealType;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        if (discountValue == null && discount != null) {
            return BigDecimal.valueOf(discount);
        }
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public Integer getDiscount() {
        if (discount != null) return discount;
        if (discountValue != null) return discountValue.intValue();
        return null;
    }

    public void setDiscount(Integer discount) {
        this.discount = discount;
        if (discount != null && this.discountValue == null) {
            this.discountValue = BigDecimal.valueOf(discount);
        }
    }

    public BigDecimal getMaxDiscountAmount() {
        return maxDiscountAmount;
    }

    public void setMaxDiscountAmount(BigDecimal maxDiscountAmount) {
        this.maxDiscountAmount = maxDiscountAmount;
    }

    public BigDecimal getMinOrderAmount() {
        return minOrderAmount;
    }

    public void setMinOrderAmount(BigDecimal minOrderAmount) {
        this.minOrderAmount = minOrderAmount;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Integer getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(Integer usageLimit) {
        this.usageLimit = usageLimit;
    }

    public List<Long> getProductIds() {
        return productIds;
    }

    public void setProductIds(List<Long> productIds) {
        this.productIds = productIds != null ? productIds : new ArrayList<>();
    }

    public String getCategorySlug() {
        return categorySlug;
    }

    public void setCategorySlug(String categorySlug) {
        this.categorySlug = categorySlug;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }
}
