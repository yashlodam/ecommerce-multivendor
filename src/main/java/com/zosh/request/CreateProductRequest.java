package com.zosh.request;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

public class CreateProductRequest {

    @NotBlank(message = "Product title is required")
    private String title;

    @NotBlank(message = "Product description is required")
    private String description;

    @Positive(message = "MRP price must be positive")
    private int mrpPrice;

    @Positive(message = "Selling price must be positive")
    private int sellingPrice;

    @Min(value = 0, message = "Quantity cannot be negative")
    private int quantity;

    private String color;

    private String brand;

    @NotEmpty(message = "At least one product image is required")
    private List<String> images;

    @NotBlank(message = "Primary category is required")
    private String category;

    private String category2;

    private String category3;

    private String sizes;

    /**
     * Optional list of variants to create alongside the product.
     * If provided, each variant gets its own price + quantity.
     * If empty or null, the service creates one default variant from the
     * top-level mrpPrice/sellingPrice/quantity/sizes fields for backward compat.
     */
    @Valid
    private List<ProductVariantRequest> variants = new ArrayList<>();

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

    public int getMrpPrice() {
        return mrpPrice;
    }

    public void setMrpPrice(int mrpPrice) {
        this.mrpPrice = mrpPrice;
    }

    public int getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(int sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategory2() {
        return category2;
    }

    public void setCategory2(String category2) {
        this.category2 = category2;
    }

    public String getCategory3() {
        return category3;
    }

    public void setCategory3(String category3) {
        this.category3 = category3;
    }

    public String getSizes() {
        return sizes;
    }

    public void setSizes(String sizes) {
        this.sizes = sizes;
    }

    public List<ProductVariantRequest> getVariants() {
        return variants;
    }

    public void setVariants(List<ProductVariantRequest> variants) {
        this.variants = variants;
    }
}
