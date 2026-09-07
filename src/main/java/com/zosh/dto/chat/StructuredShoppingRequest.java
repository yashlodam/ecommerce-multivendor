package com.zosh.dto.chat;

import com.zosh.domain.ChatIntent;

/**
 * Encapsulates structured shopping parameters parsed from natural language user messages.
 * Prevents raw sentences from being passed directly to search APIs.
 */
public class StructuredShoppingRequest {

    private ChatIntent intent = ChatIntent.PRODUCT_SEARCH;
    private String query;
    private String category;
    private String categoryName;
    private String brand;
    private Integer minPrice;
    private Integer maxPrice;
    private String color;
    private String size;
    private String sort = "relevance";
    private int page = 0;
    private int pageSize = 10;
    private boolean isFollowUpRefinement = false;

    public StructuredShoppingRequest() {}

    public ChatIntent getIntent() {
        return intent;
    }

    public void setIntent(ChatIntent intent) {
        this.intent = intent;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public Integer getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(Integer minPrice) {
        this.minPrice = minPrice;
    }

    public Integer getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(Integer maxPrice) {
        this.maxPrice = maxPrice;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public boolean isFollowUpRefinement() {
        return isFollowUpRefinement;
    }

    public void setFollowUpRefinement(boolean followUpRefinement) {
        isFollowUpRefinement = followUpRefinement;
    }

    public boolean hasCategory() {
        return category != null && !category.isBlank();
    }

    public boolean hasBrand() {
        return brand != null && !brand.isBlank();
    }

    public boolean hasPriceFilter() {
        return minPrice != null || maxPrice != null;
    }

    @Override
    public String toString() {
        return "StructuredShoppingRequest{" +
                "intent=" + intent +
                ", query=\"" + query + "\"" +
                ", category=\"" + category + "\"" +
                ", brand=\"" + brand + "\"" +
                ", minPrice=" + minPrice +
                ", maxPrice=" + maxPrice +
                ", color=\"" + color + "\"" +
                ", size=\"" + size + "\"" +
                ", sort=\"" + sort + "\"" +
                '}';
    }
}
