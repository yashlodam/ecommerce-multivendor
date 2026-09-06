package com.zosh.dto.chat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AiOrderSummaryDto {
    private Long orderId;
    private String orderStatus;
    private Integer totalAmount;
    private LocalDateTime orderDate;
    private LocalDateTime deliveryDate;
    private String shippingAddress;
    private int itemCount;
    private List<String> itemTitles = new ArrayList<>();

    public AiOrderSummaryDto() {
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public Integer getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Integer totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public LocalDateTime getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(LocalDateTime deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public List<String> getItemTitles() {
        return itemTitles;
    }

    public void setItemTitles(List<String> itemTitles) {
        this.itemTitles = itemTitles != null ? itemTitles : new ArrayList<>();
    }
}
