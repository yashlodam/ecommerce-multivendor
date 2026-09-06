package com.zosh.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "seller_reports",
    indexes = {
        @Index(name = "idx_report_seller_id", columnList = "seller_id")
    }
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SellerReport {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seller_rep_seq")
    @SequenceGenerator(name = "seller_rep_seq", sequenceName = "seller_report_sequence", allocationSize = 1)
    private Long id;

    @OneToOne(optional = false)
    private Seller seller;

    private Long totalEarnings = 0L;

    private Long totalSales = 0L;

    private Long totalRefunds = 0L;

    private Long totalTax = 0L;

    private Long netEarnings = 0L;

    private Integer totalOrders = 0;

    private Integer canceledOrders = 0;

    private Integer totalTransactions = 0;

    public SellerReport() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Seller getSeller() {
        return seller;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
    }

    public Long getTotalEarnings() {
        return totalEarnings != null ? totalEarnings : 0L;
    }

    public void setTotalEarnings(Long totalEarnings) {
        this.totalEarnings = totalEarnings;
    }

    public Long getTotalSales() {
        return totalSales != null ? totalSales : 0L;
    }

    public void setTotalSales(Long totalSales) {
        this.totalSales = totalSales;
    }

    public Long getTotalRefunds() {
        return totalRefunds != null ? totalRefunds : 0L;
    }

    public void setTotalRefunds(Long totalRefunds) {
        this.totalRefunds = totalRefunds;
    }

    public Long getTotalTax() {
        return totalTax != null ? totalTax : 0L;
    }

    public void setTotalTax(Long totalTax) {
        this.totalTax = totalTax;
    }

    public Long getNetEarnings() {
        return netEarnings != null ? netEarnings : 0L;
    }

    public void setNetEarnings(Long netEarnings) {
        this.netEarnings = netEarnings;
    }

    public Integer getTotalOrders() {
        return totalOrders != null ? totalOrders : 0;
    }

    public void setTotalOrders(Integer totalOrders) {
        this.totalOrders = totalOrders;
    }

    public Integer getCanceledOrders() {
        return canceledOrders != null ? canceledOrders : 0;
    }

    public void setCanceledOrders(Integer canceledOrders) {
        this.canceledOrders = canceledOrders;
    }

    public Integer getTotalTransactions() {
        return totalTransactions != null ? totalTransactions : 0;
    }

    public void setTotalTransactions(Integer totalTransactions) {
        this.totalTransactions = totalTransactions;
    }
}
