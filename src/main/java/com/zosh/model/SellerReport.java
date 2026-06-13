package com.zosh.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

@Entity
public class SellerReport {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	
	@OneToOne
	private Seller seller;
	
	private Long totalEarnings = 0L;
	
	private Long totalSales =0L;
	
	private Long totalRefunds = 0L;
	
	private Long totalTax = 0L;
	
	private Long netEarnings = 0L;
	
	private Integer totalOrders = 0;
	
	private Integer canceledOrders = 0;
	
	private Integer totalTransactions = 0;

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
		return totalEarnings;
	}

	public void setTotalEarnings(Long totalEarnings) {
		this.totalEarnings = totalEarnings;
	}

	public Long getTotalSales() {
		return totalSales;
	}

	public void setTotalSales(Long totalSales) {
		this.totalSales = totalSales;
	}

	public Long getTotalRefunds() {
		return totalRefunds;
	}

	public void setTotalRefunds(Long totalRefunds) {
		this.totalRefunds = totalRefunds;
	}

	public Long getTotalTax() {
		return totalTax;
	}

	public void setTotalTax(Long totalTax) {
		this.totalTax = totalTax;
	}

	public Long getNetEarnings() {
		return netEarnings;
	}

	public void setNetEarnings(Long netEarnings) {
		this.netEarnings = netEarnings;
	}

	public Integer getTotalOrders() {
		return totalOrders;
	}

	public void setTotalOrders(Integer totalOrders) {
		this.totalOrders = totalOrders;
	}

	public Integer getCanceledOrders() {
		return canceledOrders;
	}

	public void setCanceledOrders(Integer canceledOrders) {
		this.canceledOrders = canceledOrders;
	}

	public Integer getTotalTransactions() {
		return totalTransactions;
	}

	public void setTotalTransactions(Integer totalTransactions) {
		this.totalTransactions = totalTransactions;
	}
	
	
	
}
