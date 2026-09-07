package com.zosh.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.zosh.domain.PaymentMethod;
import com.zosh.domain.PaymentOrderStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "payment_orders",
    indexes = {
        @Index(name = "idx_pay_order_link_id", columnList = "paymentLinkId"),
        @Index(name = "idx_pay_order_rzp_order_id", columnList = "razorpayOrderId"),
        @Index(name = "idx_pay_order_user_id", columnList = "user_id"),
        @Index(name = "idx_pay_order_status", columnList = "status")
    }
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pay_order_seq")
    @SequenceGenerator(name = "pay_order_seq", sequenceName = "payment_order_sequence", allocationSize = 1)
    private Long id;

    private Long amount;

    @Enumerated(EnumType.STRING)
    private PaymentOrderStatus status = PaymentOrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private String paymentLinkId;

    private String razorpayOrderId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"addresses", "usedCoupons", "password", "hibernateLazyInitializer", "handler"})
    private User user;

    @ManyToOne
    @JoinColumn(name = "shipping_address_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Address shippingAddress;

    public PaymentOrder() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public PaymentOrderStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentOrderStatus status) {
        this.status = status;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentLinkId() {
        return paymentLinkId;
    }

    public void setPaymentLinkId(String paymentLinkId) {
        this.paymentLinkId = paymentLinkId;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Address getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(Address shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
}