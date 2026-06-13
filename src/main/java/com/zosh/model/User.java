package com.zosh.model;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zosh.domain.USER_ROLE;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;

@Entity
public class User {

	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private String password;
	
	
	private String email;
	
	private String fullName;
	
	private String mobile;
	
	
	private USER_ROLE role = USER_ROLE.ROLE_CUSTOMER;
	
	
	@OneToMany
	private Set<Address> addresses = new HashSet<>();
	
	@ManyToMany
	@JsonIgnore
	private Set<Coupon> usedCoupons = new HashSet<>();
	
	 public User() {
		 
	 }
	 
	
	
	
}
