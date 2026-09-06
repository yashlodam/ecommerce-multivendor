package com.zosh.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

/**
 * Represents a specific purchasable variant of a product.
 *
 * Examples:
 *   - A T-Shirt product may have variants: S (qty=10), M (qty=5), L (qty=0)
 *   - A Smartphone product may have variants: 128GB (qty=3), 256GB (qty=8)
 *   - A product with no real variants gets one isDefault=true variant named "Standard"
 *
 * Each variant has its own price and quantity so that inventory and pricing
 * can differ per option (e.g. 256GB costs more than 128GB).
 *
 * The @Version field provides optimistic locking — prevents two concurrent
 * checkouts from overselling the same variant.
 */
@Entity
@Table(
    name = "product_variants",
    indexes = {
        @Index(name = "idx_variant_product_id", columnList = "product_id"),
        @Index(name = "idx_variant_sku",        columnList = "sku")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_variant_product_name",
                          columnNames = {"product_id", "variant_name"})
    }
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_variant_seq")
    @SequenceGenerator(name = "product_variant_seq",
                       sequenceName = "product_variant_sequence",
                       allocationSize = 1)
    private Long id;

    /** The product this variant belongs to */
    @JsonIgnore
    @ManyToOne(optional = false)
    private Product product;

    /**
     * The display label for this variant option.
     * Examples: "S", "M", "L", "XL", "128GB", "256GB", "UK 8", "Standard"
     */
    @Column(name = "variant_name", nullable = false)
    private String variantName;

    /**
     * Optional Stock Keeping Unit — unique identifier used by the seller.
     * Null is allowed for auto-migrated products.
     */
    @Column(unique = true)
    private String sku;

    /** MRP price for this specific variant (in smallest currency unit / paisa) */
    @Column(nullable = false)
    private Integer mrpPrice;

    /** Selling price for this specific variant */
    @Column(nullable = false)
    private Integer sellingPrice;

    /** Computed discount percentage for this variant */
    private Integer discountPercent;

    /** Available stock for this specific variant */
    @Column(nullable = false)
    private Integer quantity = 0;

    /**
     * True for the single auto-generated variant of products that don't
     * have real size/storage variants (e.g. furniture, accessories).
     * Used by the frontend to skip showing a variant selector.
     */
    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Optimistic lock — prevents two concurrent checkouts from both
     * decrementing the last unit of this variant.
     */
    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // ---- Getters and Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public String getVariantName() { return variantName; }
    public void setVariantName(String variantName) { this.variantName = variantName; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public Integer getMrpPrice() { return mrpPrice; }
    public void setMrpPrice(Integer mrpPrice) { this.mrpPrice = mrpPrice; }

    public Integer getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(Integer sellingPrice) { this.sellingPrice = sellingPrice; }

    public Integer getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Integer discountPercent) { this.discountPercent = discountPercent; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getVersion() { return version; }
}
