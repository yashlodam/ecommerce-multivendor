package com.zosh.dto.chat;

import java.util.ArrayList;
import java.util.List;

public class AiCartSummaryDto {
    private int totalItem;
    private double totalSellingPrice;
    private int totalMrpPrice;
    private int discount;
    private List<AiCartItemDto> items = new ArrayList<>();

    public AiCartSummaryDto() {
    }

    public static class AiCartItemDto {
        private Long cartItemId;
        private Long productId;
        private Long variantId;
        private String productTitle;
        private String variantName;
        private int quantity;
        private Integer sellingPrice;

        public AiCartItemDto() {
        }

        public AiCartItemDto(Long cartItemId, Long productId, Long variantId, String productTitle, String variantName, int quantity, Integer sellingPrice) {
            this.cartItemId = cartItemId;
            this.productId = productId;
            this.variantId = variantId;
            this.productTitle = productTitle;
            this.variantName = variantName;
            this.quantity = quantity;
            this.sellingPrice = sellingPrice;
        }

        public Long getCartItemId() {
            return cartItemId;
        }

        public void setCartItemId(Long cartItemId) {
            this.cartItemId = cartItemId;
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

        public String getProductTitle() {
            return productTitle;
        }

        public void setProductTitle(String productTitle) {
            this.productTitle = productTitle;
        }

        public String getVariantName() {
            return variantName;
        }

        public void setVariantName(String variantName) {
            this.variantName = variantName;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public Integer getSellingPrice() {
            return sellingPrice;
        }

        public void setSellingPrice(Integer sellingPrice) {
            this.sellingPrice = sellingPrice;
        }
    }

    public int getTotalItem() {
        return totalItem;
    }

    public void setTotalItem(int totalItem) {
        this.totalItem = totalItem;
    }

    public double getTotalSellingPrice() {
        return totalSellingPrice;
    }

    public void setTotalSellingPrice(double totalSellingPrice) {
        this.totalSellingPrice = totalSellingPrice;
    }

    public int getTotalMrpPrice() {
        return totalMrpPrice;
    }

    public void setTotalMrpPrice(int totalMrpPrice) {
        this.totalMrpPrice = totalMrpPrice;
    }

    public int getDiscount() {
        return discount;
    }

    public void setDiscount(int discount) {
        this.discount = discount;
    }

    public List<AiCartItemDto> getItems() {
        return items;
    }

    public void setItems(List<AiCartItemDto> items) {
        this.items = items != null ? items : new ArrayList<>();
    }
}
