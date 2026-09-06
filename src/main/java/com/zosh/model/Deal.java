package com.zosh.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.zosh.domain.DealStatus;
import com.zosh.domain.DealType;
import com.zosh.domain.DiscountType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Enterprise Promotional Deal / Discount entity.
 * Supports PRODUCT, CATEGORY, SELLER, and ORDER-level deals.
 */
@Entity
@Table(
    name = "deals",
    indexes = {
        @Index(name = "idx_deal_category_id", columnList = "category_id"),
        @Index(name = "idx_deal_seller_id", columnList = "seller_id"),
        @Index(name = "idx_deal_active", columnList = "active"),
        @Index(name = "idx_deal_start_end", columnList = "start_at, end_at"),
        @Index(name = "idx_deal_type", columnList = "deal_type")
    }
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Deal {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "deal_seq")
    @SequenceGenerator(name = "deal_seq", sequenceName = "deal_sequence", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String title = "Promotional Deal";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "deal_type", nullable = false)
    private DealType dealType = DealType.CATEGORY;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType = DiscountType.PERCENTAGE;

    @Column(name = "discount_value", precision = 10, scale = 2, nullable = false)
    private BigDecimal discountValue = BigDecimal.valueOf(10);

    /** Optional maximum discount cap for percentage-based deals (e.g. 20% max ₹500) */
    @Column(name = "max_discount_amount", precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;

    /** Optional minimum order or item price required to qualify */
    @Column(name = "min_order_amount", precision = 10, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 0;

    /** Seller owner (null if created by Admin as marketplace-wide deal) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    @JsonIgnore
    private Seller seller;

    /** Optional category binding for category deals & homepage display */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private HomeCategory category;

    /** Optional category slug string for direct catalog category matching */
    @Column(name = "category_slug")
    private String categorySlug;

    /** Product list for PRODUCT-level deals */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "deal_products",
        joinColumns = @JoinColumn(name = "deal_id"),
        inverseJoinColumns = @JoinColumn(name = "product_id"),
        indexes = {
            @Index(name = "idx_deal_products_deal", columnList = "deal_id"),
            @Index(name = "idx_deal_products_product", columnList = "product_id")
        }
    )
    @JsonIgnore
    private List<Product> products = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Legacy column support
    @Transient
    private Integer discount;

    public Deal() {
    }

    public Deal(Long id, Integer discount, HomeCategory category) {
        this.id = id;
        this.discount = discount;
        this.discountValue = discount != null ? BigDecimal.valueOf(discount) : BigDecimal.valueOf(10);
        this.category = category;
        this.title = category != null ? (category.getName() + " Deal") : "Promotional Deal";
        this.dealType = DealType.CATEGORY;
        this.discountType = DiscountType.PERCENTAGE;
        this.startAt = LocalDateTime.now();
        this.endAt = LocalDateTime.now().plusYears(1);
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (startAt == null) startAt = LocalDateTime.now();
        if (endAt == null) endAt = LocalDateTime.now().plusMonths(3);
        if (dealType == null) dealType = (category != null ? DealType.CATEGORY : DealType.PRODUCT);
        if (discountType == null) discountType = DiscountType.PERCENTAGE;
        if (discountValue == null) {
            discountValue = discount != null ? BigDecimal.valueOf(discount) : BigDecimal.valueOf(10);
        }
        if (title == null || title.isBlank()) {
            title = category != null ? (category.getName() + " Deal") : "Special Promotion";
        }
        if (categorySlug == null && category != null) {
            categorySlug = category.getCategoryId();
        }
        if (usageCount == null) usageCount = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (categorySlug == null && category != null) {
            categorySlug = category.getCategoryId();
        }
    }

    // ---- Business Helper Methods ----

    @Transient
    public boolean isCurrentlyActive() {
        if (!active) return false;
        LocalDateTime now = LocalDateTime.now();
        if (startAt != null && now.isBefore(startAt)) return false;
        if (endAt != null && now.isAfter(endAt)) return false;
        if (usageLimit != null && usageCount != null && usageCount >= usageLimit) return false;
        return true;
    }

    @Transient
    public DealStatus getDerivedStatus() {
        if (!active) return DealStatus.DISABLED;
        LocalDateTime now = LocalDateTime.now();
        if (startAt != null && now.isBefore(startAt)) return DealStatus.SCHEDULED;
        if (endAt != null && now.isAfter(endAt)) return DealStatus.EXPIRED;
        if (usageLimit != null && usageCount != null && usageCount >= usageLimit) return DealStatus.DEPLETED;
        return DealStatus.ACTIVE;
    }

    // ---- Getters and Setters ----

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
        if (discountValue != null) {
            this.discount = discountValue.intValue();
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Integer getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(Integer usageLimit) {
        this.usageLimit = usageLimit;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }

    public Seller getSeller() {
        return seller;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
    }

    public HomeCategory getCategory() {
        return category;
    }

    public void setCategory(HomeCategory category) {
        this.category = category;
        if (category != null && (this.categorySlug == null || this.categorySlug.isBlank())) {
            this.categorySlug = category.getCategoryId();
        }
    }

    public String getCategorySlug() {
        return categorySlug;
    }

    public void setCategorySlug(String categorySlug) {
        this.categorySlug = categorySlug;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // ---- Legacy Backwards Compatibility ----

    public Integer getDiscount() {
        if (discountValue != null) {
            return discountValue.intValue();
        }
        return discount != null ? discount : 0;
    }

    public void setDiscount(Integer discount) {
        this.discount = discount;
        if (discount != null) {
            this.discountValue = BigDecimal.valueOf(discount);
        }
    }
}
