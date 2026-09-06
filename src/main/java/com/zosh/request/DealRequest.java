package com.zosh.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public class DealRequest {

    @NotNull(message = "Discount is required")
    @Min(value = 1, message = "Discount must be at least 1%")
    @Max(value = 99, message = "Discount cannot exceed 99%")
    private Integer discount;

    private Long categoryId;

    private String categorySlug;

    public Integer getDiscount() {
        return discount;
    }

    public void setDiscount(Integer discount) {
        this.discount = discount;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategorySlug() {
        return categorySlug;
    }

    public void setCategorySlug(String categorySlug) {
        this.categorySlug = categorySlug;
    }

    @JsonProperty("categoryId")
    public void setCategoryIdFlexible(Object val) {
        if (val instanceof Number) {
            this.categoryId = ((Number) val).longValue();
        } else if (val instanceof String) {
            String str = ((String) val).trim();
            try {
                this.categoryId = Long.parseLong(str);
            } catch (NumberFormatException e) {
                this.categorySlug = str;
            }
        }
    }

    @JsonProperty("category")
    public void setCategoryFlexible(Object category) {
        if (category instanceof Number) {
            this.categoryId = ((Number) category).longValue();
        } else if (category instanceof String) {
            String str = ((String) category).trim();
            try {
                this.categoryId = Long.parseLong(str);
            } catch (NumberFormatException e) {
                this.categorySlug = str;
            }
        } else if (category instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) category;
            Object idObj = map.get("id");
            if (idObj instanceof Number) {
                this.categoryId = ((Number) idObj).longValue();
            } else if (idObj != null) {
                try {
                    this.categoryId = Long.parseLong(idObj.toString().trim());
                } catch (NumberFormatException ignored) {}
            }
            Object slugObj = map.get("categoryId");
            if (slugObj != null) {
                this.categorySlug = slugObj.toString().trim();
            }
        }
    }

    @AssertTrue(message = "Category identifier is required (provide categoryId or target category)")
    public boolean isCategoryProvided() {
        return categoryId != null || (categorySlug != null && !categorySlug.trim().isEmpty());
    }
}
