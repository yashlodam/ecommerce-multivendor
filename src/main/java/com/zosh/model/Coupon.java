package com.zosh.model;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

@Entity
public class Coupon {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	
	private String code;
	
	private double discountPercentage;
	
	private LocalDate validityStartDate;
	
	private LocalDate validityEndDate;
	
	private double minimumOrderValue;
	
	private boolean isActive = true;
	
	@ManyToMany(mappedBy = "usedCoupons")
	private Set<User> usedByUsers = new HashSet<>();
	
	 public String getCode() {
		return code;
	}

	public Set<User> getUsedByUsers() {
		return usedByUsers;
	}

	 public void setUsedByUsers(Set<User> usedByUsers) {
		 this.usedByUsers = usedByUsers;
	 }

	public void setCode(String code) {
		this.code = code;
	}

	public double getDiscountPercentage() {
		return discountPercentage;
	}

	public void setDiscountPercentage(double discountPercentage) {
		this.discountPercentage = discountPercentage;
	}

	public LocalDate getValidityStartDate() {
		return validityStartDate;
	}

	public void setValidityStartDate(LocalDate validityStartDate) {
		this.validityStartDate = validityStartDate;
	}

	public LocalDate getValidityEndDate() {
		return validityEndDate;
	}

	public void setValidityEndDate(LocalDate validityEndDate) {
		this.validityEndDate = validityEndDate;
	}

	public double getMinimumOrderValue() {
		return minimumOrderValue;
	}

	public void setMinimumOrderValue(double minimumOrderValue) {
		this.minimumOrderValue = minimumOrderValue;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	 public Coupon() {
	    }

	 public Long getId() {
		 return id;
	 }

	 public void setId(Long id) {
		 this.id = id;
	 }
	 
	 
	
	
}
