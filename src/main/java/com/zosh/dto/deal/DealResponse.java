package com.zosh.dto.deal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.zosh.domain.DealStatus;
import com.zosh.domain.DealType;
import com.zosh.domain.DiscountType;
import com.zosh.model.Deal;
import com.zosh.model.Product;

public class DealResponse {

    private Long id;
    private String title;
    private String description;
    private DealType dealType;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private boolean active;
    private DealStatus status;
    private Integer usageLimit;
    private Integer usageCount;

    private Long sellerId;
    private String sellerName;

    private String categorySlug;
    private String categoryName;
    private CategorySummary category;

    private List<ProductSummary> products = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static class CategorySummary {
        private Long id;
        private String categoryId;
        private String name;
        private String image;

        public CategorySummary() {}

        public CategorySummary(Long id, String categoryId, String name, String image) {
            this.id = id;
            this.categoryId = categoryId;
            this.name = name;
            this.image = image;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getCategoryId() { return categoryId; }
        public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getImage() { return image; }
        public void setImage(String image) { this.image = image; }
    }

    public static class ProductSummary {
        private Long id;
        private String title;
        private Integer mrpPrice;
        private Integer sellingPrice;
        private String image;

        public ProductSummary() {}

        public ProductSummary(Long id, String title, Integer mrpPrice, Integer sellingPrice, String image) {
            this.id = id;
            this.title = title;
            this.mrpPrice = mrpPrice;
            this.sellingPrice = sellingPrice;
            this.image = image;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public Integer getMrpPrice() { return mrpPrice; }
        public void setMrpPrice(Integer mrpPrice) { this.mrpPrice = mrpPrice; }
        public Integer getSellingPrice() { return sellingPrice; }
        public void setSellingPrice(Integer sellingPrice) { this.sellingPrice = sellingPrice; }
        public String getImage() { return image; }
        public void setImage(String image) { this.image = image; }
    }

    public DealResponse() {
    }

    public static DealResponse fromEntity(Deal deal) {
        if (deal == null) return null;
        DealResponse res = new DealResponse();
        res.setId(deal.getId());
        res.setTitle(deal.getTitle());
        res.setDescription(deal.getDescription());
        res.setDealType(deal.getDealType());
        res.setDiscountType(deal.getDiscountType());
        res.setDiscountValue(deal.getDiscountValue());
        res.setMaxDiscountAmount(deal.getMaxDiscountAmount());
        res.setMinOrderAmount(deal.getMinOrderAmount());
        res.setStartAt(deal.getStartAt());
        res.setEndAt(deal.getEndAt());
        res.setActive(deal.isActive());
        res.setStatus(deal.getDerivedStatus());
        res.setUsageLimit(deal.getUsageLimit());
        res.setUsageCount(deal.getUsageCount() != null ? deal.getUsageCount() : 0);
        res.setCreatedAt(deal.getCreatedAt());
        res.setUpdatedAt(deal.getUpdatedAt());

        if (deal.getSeller() != null) {
            try {
                if (org.hibernate.Hibernate.isInitialized(deal.getSeller())) {
                    res.setSellerId(deal.getSeller().getId());
                    res.setSellerName(deal.getSeller().getSellerName());
                }
            } catch (Exception ignored) {}
        }

        if (deal.getCategory() != null) {
            try {
                if (org.hibernate.Hibernate.isInitialized(deal.getCategory())) {
                    res.setCategorySlug(deal.getCategory().getCategoryId());
                    res.setCategoryName(deal.getCategory().getName());
                    res.setCategory(new CategorySummary(
                            deal.getCategory().getId(),
                            deal.getCategory().getCategoryId(),
                            deal.getCategory().getName(),
                            deal.getCategory().getImage()
                    ));
                } else if (deal.getCategorySlug() != null) {
                    res.setCategorySlug(deal.getCategorySlug());
                    res.setCategory(new CategorySummary(
                            null,
                            deal.getCategorySlug(),
                            deal.getCategorySlug(),
                            null
                    ));
                }
            } catch (Exception ignored) {
                if (deal.getCategorySlug() != null) {
                    res.setCategorySlug(deal.getCategorySlug());
                }
            }
        } else if (deal.getCategorySlug() != null) {
            res.setCategorySlug(deal.getCategorySlug());
            res.setCategory(new CategorySummary(
                    null,
                    deal.getCategorySlug(),
                    deal.getCategorySlug(),
                    null
            ));
        }

        if (deal.getProducts() != null) {
            try {
                if (org.hibernate.Hibernate.isInitialized(deal.getProducts())) {
                    for (Product p : deal.getProducts()) {
                        String img = (p.getImages() != null && !p.getImages().isEmpty()) ? p.getImages().get(0) : null;
                        res.getProducts().add(new ProductSummary(
                                p.getId(), p.getTitle(), p.getMrpPrice(), p.getSellingPrice(), img));
                    }
                }
            } catch (Exception ignored) {}
        }

        return res;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public DealType getDealType() { return dealType; }
    public void setDealType(DealType dealType) { this.dealType = dealType; }
    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }
    public BigDecimal getMaxDiscountAmount() { return maxDiscountAmount; }
    public void setMaxDiscountAmount(BigDecimal maxDiscountAmount) { this.maxDiscountAmount = maxDiscountAmount; }
    public BigDecimal getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(BigDecimal minOrderAmount) { this.minOrderAmount = minOrderAmount; }
    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public DealStatus getStatus() { return status; }
    public void setStatus(DealStatus status) { this.status = status; }
    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }
    public Integer getUsageCount() { return usageCount; }
    public void setUsageCount(Integer usageCount) { this.usageCount = usageCount; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public String getCategorySlug() { return categorySlug; }
    public void setCategorySlug(String categorySlug) { this.categorySlug = categorySlug; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public List<ProductSummary> getProducts() { return products; }
    public void setProducts(List<ProductSummary> products) { this.products = products; }
    public CategorySummary getCategory() { return category; }
    public void setCategory(CategorySummary category) { this.category = category; }
    public Integer getDiscount() { return discountValue != null ? discountValue.intValue() : null; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
