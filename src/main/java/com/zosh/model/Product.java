package com.zosh.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;


/**
 * Represents a product listed by a seller.
 *
 * Price is stored as Integer (paisa / smallest currency unit) to avoid
 * floating-point precision issues with money.
 *
 * The @Version field enables optimistic locking — if two transactions try to
 * update the same product concurrently (e.g., two buyers checking out the last
 * item), one will succeed and the other will receive an OptimisticLockException,
 * preventing overselling.
 */
@Entity
@Table(
    name = "products",
    indexes = {
        @Index(name = "idx_product_seller",   columnList = "seller_id"),
        @Index(name = "idx_product_category", columnList = "category_id"),
        @Index(name = "idx_product_created",  columnList = "created_at")
    }
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_seq")
    @SequenceGenerator(name = "product_seq", sequenceName = "product_sequence", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Price stored as Integer in smallest currency unit (e.g. paisa for INR) */
    @Column(nullable = false)
    private Integer mrpPrice;

    @Column(nullable = false)
    private Integer sellingPrice;

    private Integer discountPercent;

    /** Available stock quantity — protected by optimistic locking via @Version */
    @Column(nullable = false)
    private Integer quantity = 0;

    private String color;

    private String brand;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> images = new ArrayList<>();

    private Integer numRatings = 0;

    @ManyToOne
    private Category category;

    @ManyToOne(optional = false)
    private Seller seller;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private String sizes;

    /**
     * Optimistic lock version — Hibernate increments this on every UPDATE.
     * Prevents two concurrent transactions from both deducting the last item.
     */
    @Version
    private Long version;

    @JsonIgnore
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();

    /**
     * All purchasable variants of this product (sizes, storage options, etc.).
     * Products with no real variants have exactly one isDefault=true variant.
     * Lazily loaded to avoid N+1 on catalog queries.
     */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ProductVariant> variants = new ArrayList<>();

    @Transient
    private Boolean dealActive = false;

    @Transient
    private Integer effectivePrice;

    @Transient
    private Integer discountAmount = 0;

    @Transient
    private String appliedDealTitle;

    @Transient
    private LocalDateTime dealEndsAt;

    @PrePersist

    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // ---- Getters and Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getMrpPrice() { return mrpPrice; }
    public void setMrpPrice(Integer mrpPrice) { this.mrpPrice = mrpPrice; }

    public Integer getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(Integer sellingPrice) { this.sellingPrice = sellingPrice; }

    public Integer getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Integer discountPercent) { this.discountPercent = discountPercent; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public Integer getNumRatings() { return numRatings; }
    public void setNumRatings(Integer numRatings) { this.numRatings = numRatings; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public Seller getSeller() { return seller; }
    public void setSeller(Seller seller) { this.seller = seller; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getSizes() { return sizes; }
    public void setSizes(String sizes) { this.sizes = sizes; }

    public Long getVersion() { return version; }

    public List<Review> getReviews() { return reviews; }
    public void setReviews(List<Review> reviews) { this.reviews = reviews; }

    public List<ProductVariant> getVariants() { return variants; }
    public void setVariants(List<ProductVariant> variants) { this.variants = variants; }

    /**
     * Returns the total available stock across all variants.
     * Falls back to the flat {@code quantity} field if no variants exist yet
     * (backward compatibility with pre-migration products).
     */
    public int getTotalStock() {
        if (variants == null || variants.isEmpty()) {
            return quantity != null ? quantity : 0;
        }
        return variants.stream()
                       .mapToInt(v -> v.getQuantity() != null ? v.getQuantity() : 0)
                       .sum();
    }

    public Boolean getDealActive() { return dealActive; }
    public void setDealActive(Boolean dealActive) { this.dealActive = dealActive; }

    public Integer getEffectivePrice() { return effectivePrice != null ? effectivePrice : sellingPrice; }
    public void setEffectivePrice(Integer effectivePrice) { this.effectivePrice = effectivePrice; }

    public Integer getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(Integer discountAmount) { this.discountAmount = discountAmount; }

    public String getAppliedDealTitle() { return appliedDealTitle; }
    public void setAppliedDealTitle(String appliedDealTitle) { this.appliedDealTitle = appliedDealTitle; }

    public LocalDateTime getDealEndsAt() { return dealEndsAt; }
    public void setDealEndsAt(LocalDateTime dealEndsAt) { this.dealEndsAt = dealEndsAt; }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return id != null && id.equals(product.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
