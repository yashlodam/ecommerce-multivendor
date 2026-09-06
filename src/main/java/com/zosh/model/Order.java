package com.zosh.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.zosh.domain.OrderStatus;
import com.zosh.domain.PaymentStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/**
 * Represents a vendor-specific sub-order within a customer checkout.
 *
 * When a customer checks out with items from multiple vendors, one Order is
 * created per vendor. The PaymentOrder ties them together under one payment.
 *
 * Order lifecycle:
 *   PENDING → PLACED → CONFIRMED → SHIPPED → DELIVERED
 *   Any state → CANCELLED (with business rule constraints)
 */
@Entity
@Table(
    name = "orders",
    indexes = {
        @Index(name = "idx_order_user_id",   columnList = "user_id"),
        @Index(name = "idx_order_seller_id", columnList = "seller_id"),
        @Index(name = "idx_order_status",    columnList = "order_status"),
        @Index(name = "idx_order_payment_status", columnList = "payment_status")
    }
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_seq")
    @SequenceGenerator(name = "order_seq", sequenceName = "order_sequence", allocationSize = 1)
    private Long id;

    /** Human-readable order reference (e.g. ORD-20240901-00001) */
    @Column(unique = true)
    private String orderId;

    @ManyToOne(optional = false)
    @JsonIgnoreProperties({"addresses", "usedCoupons", "password", "hibernateLazyInitializer", "handler"})
    private User user;

    /** Denormalized seller ID for fast querying without join to OrderItems */
    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> orderItems = new ArrayList<>();

    @ManyToOne
    private Address shippingAddress;

    @Embedded
    private PaymentDetails paymentDetails = new PaymentDetails();

    /** Original total MRP price (before discounts) — stored in paisa/smallest unit */
    private Integer totalMrpPrice;

    /** Actual amount paid by customer — stored in paisa/smallest unit */
    private Integer totalSellingPrice;

    private Integer discount;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus = OrderStatus.PENDING;

    private int totalItems;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private LocalDateTime orderDate;

    private LocalDateTime deliverDate;

    @PrePersist
    protected void onCreate() {
        if (orderDate == null) {
            orderDate = LocalDateTime.now();
        }
        if (deliverDate == null) {
            deliverDate = orderDate.plusDays(7);
        }
    }

    // ---- Getters and Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }

    public List<OrderItem> getOrderItems() { return orderItems; }
    public void setOrderItems(List<OrderItem> orderItems) { this.orderItems = orderItems; }

    public Address getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(Address shippingAddress) { this.shippingAddress = shippingAddress; }

    public PaymentDetails getPaymentDetails() { return paymentDetails; }
    public void setPaymentDetails(PaymentDetails paymentDetails) { this.paymentDetails = paymentDetails; }

    public Integer getTotalMrpPrice() { return totalMrpPrice; }
    public void setTotalMrpPrice(Integer totalMrpPrice) { this.totalMrpPrice = totalMrpPrice; }

    public Integer getTotalSellingPrice() { return totalSellingPrice; }
    public void setTotalSellingPrice(Integer totalSellingPrice) { this.totalSellingPrice = totalSellingPrice; }

    public Integer getDiscount() { return discount; }
    public void setDiscount(Integer discount) { this.discount = discount; }

    public OrderStatus getOrderStatus() { return orderStatus; }
    public void setOrderStatus(OrderStatus orderStatus) { this.orderStatus = orderStatus; }

    public int getTotalItems() { return totalItems; }
    public void setTotalItems(int totalItems) { this.totalItems = totalItems; }

    /** Fixed typo: was getPaymenntStatus (double 'n') */
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public LocalDateTime getDeliverDate() { return deliverDate; }
    public void setDeliverDate(LocalDateTime deliverDate) { this.deliverDate = deliverDate; }
}
