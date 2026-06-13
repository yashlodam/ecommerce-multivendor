package com.zosh.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.zosh.domain.OrderStatus;
import com.zosh.domain.PaymentStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	private String orderId;
	
	@ManyToOne
	private User user;
	
	private Long sellerId;
	
	@OneToMany(mappedBy = "order", cascade =  CascadeType.ALL, orphanRemoval = true)
	private List<OrderItem> orderItems = new ArrayList<>();
	
	@ManyToOne
	private Address shippingAddress;
	
	@Embedded
	private PaymentDetails paymentDetails = new PaymentDetails();
	
	private double totalMrpPrice;
	
	private Integer totalSellingPrice;
	
	private Integer discount;
	
	private OrderStatus orderStatus;
	
	private int totalItems;
	
	
	private PaymentStatus paymenntStatus = PaymentStatus.PENDING;
	
	
	private LocalDateTime orderDate = LocalDateTime.now();
	private LocalDateTime deliverDate = orderDate.plusDays(7);
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getOrderId() {
		return orderId;
	}
	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}
	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}
	public Long getSellerId() {
		return sellerId;
	}
	public void setSellerId(Long sellerId) {
		this.sellerId = sellerId;
	}
	public List<OrderItem> getOrderItems() {
		return orderItems;
	}
	public void setOrderItems(List<OrderItem> orderItems) {
		this.orderItems = orderItems;
	}
	public Address getShippingAddress() {
		return shippingAddress;
	}
	public void setShippingAddress(Address shippingAddress) {
		this.shippingAddress = shippingAddress;
	}
	public PaymentDetails getPaymentDetails() {
		return paymentDetails;
	}
	public void setPaymentDetails(PaymentDetails paymentDetails) {
		this.paymentDetails = paymentDetails;
	}
	public double getTotalMrpPrice() {
		return totalMrpPrice;
	}
	public void setTotalMrpPrice(double totalMrpPrice) {
		this.totalMrpPrice = totalMrpPrice;
	}
	public Integer getTotalSellingPrice() {
		return totalSellingPrice;
	}
	public void setTotalSellingPrice(Integer totalSellingPrice) {
		this.totalSellingPrice = totalSellingPrice;
	}
	public Integer getDiscount() {
		return discount;
	}
	public void setDiscount(Integer discount) {
		this.discount = discount;
	}
	public OrderStatus getOrderStatus() {
		return orderStatus;
	}
	public void setOrderStatus(OrderStatus orderStatus) {
		this.orderStatus = orderStatus;
	}
	public int getTotalItems() {
		return totalItems;
	}
	public void setTotalItems(int totalItems) {
		this.totalItems = totalItems;
	}
	public PaymentStatus getPaymenntStatus() {
		return paymenntStatus;
	}
	public void setPaymenntStatus(PaymentStatus paymenntStatus) {
		this.paymenntStatus = paymenntStatus;
	}
	public LocalDateTime getOrderDate() {
		return orderDate;
	}
	public void setOrderDate(LocalDateTime orderDate) {
		this.orderDate = orderDate;
	}
	public LocalDateTime getDeliverDate() {
		return deliverDate;
	}
	public void setDeliverDate(LocalDateTime deliverDate) {
		this.deliverDate = deliverDate;
	}
	
	
	
	
}
