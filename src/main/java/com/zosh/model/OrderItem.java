package com.zosh.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "order_items",
    indexes = {
        @Index(name = "idx_order_item_order_id", columnList = "order_id"),
        @Index(name = "idx_order_item_product_id", columnList = "product_id"),
        @Index(name = "idx_order_item_user_id", columnList = "userId")
    }
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_item_seq")
    @SequenceGenerator(name = "order_item_seq", sequenceName = "order_item_sequence", allocationSize = 1)
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false)
    private Order order;

    @ManyToOne(optional = false)
    private Product product;

    /**
     * The specific variant ordered (nullable for backward compat with pre-variant orders).
     * When present, this records exactly which size/storage option was purchased.
     */
    @ManyToOne(optional = true)
    private ProductVariant variant;

    private String size;

    private int quantity;

    private Integer mrpPrice;

    private Integer sellingPrice;

    /** Historical pricing snapshot fields to preserve accurate order records */
    private Integer basePrice;

    private Integer discountAmount;

    private Integer finalUnitPrice;

    private Long appliedDealId;

    private String appliedDealTitle;

    private Long userId;

    public OrderItem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public ProductVariant getVariant() {
        return variant;
    }

    public void setVariant(ProductVariant variant) {
        this.variant = variant;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Integer getMrpPrice() {
        return mrpPrice;
    }

    public void setMrpPrice(Integer mrpPrice) {
        this.mrpPrice = mrpPrice;
    }

    public Integer getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(Integer sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public Integer getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(Integer basePrice) {
        this.basePrice = basePrice;
    }

    public Integer getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Integer discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Integer getFinalUnitPrice() {
        return finalUnitPrice;
    }

    public void setFinalUnitPrice(Integer finalUnitPrice) {
        this.finalUnitPrice = finalUnitPrice;
    }

    public Long getAppliedDealId() {
        return appliedDealId;
    }

    public void setAppliedDealId(Long appliedDealId) {
        this.appliedDealId = appliedDealId;
    }

    public String getAppliedDealTitle() {
        return appliedDealTitle;
    }

    public void setAppliedDealTitle(String appliedDealTitle) {
        this.appliedDealTitle = appliedDealTitle;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
