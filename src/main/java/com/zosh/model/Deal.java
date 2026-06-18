package com.zosh.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

@Entity
public class Deal {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	
	private Integer discount;
	
	@OneToOne
	private HomeCategory category;
	
    

	public Deal(Long id, Integer discount, HomeCategory category) {
		super();
		this.id = id;
		this.discount = discount;
		this.category = category;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getDiscount() {
		return discount;
	}

	public void setDiscount(Integer discount) {
		this.discount = discount;
	}

	public HomeCategory getCategory() {
		return category;
	}

	public void setCategory(HomeCategory category) {
		this.category = category;
	}
	
}
