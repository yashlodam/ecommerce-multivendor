package com.zosh.model;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zosh.domain.USER_ROLE;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private String password;
	
	@Column(unique = true, nullable = false)
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

	 public Long getId() {
		 return id;
	 }

	 public void setId(Long id) {
		 this.id = id;
	 }

	 public String getPassword() {
		 return password;
	 }

	 public void setPassword(String password) {
		 this.password = password;
	 }

	 public String getEmail() {
		 return email;
	 }

	 public void setEmail(String email) {
		 this.email = email;
	 }

	 public String getFullName() {
		 return fullName;
	 }

	 public void setFullName(String fullName) {
		 this.fullName = fullName;
	 }

	 public String getMobile() {
		 return mobile;
	 }

	 public void setMobile(String mobile) {
		 this.mobile = mobile;
	 }

	 public USER_ROLE getRole() {
		 return role;
	 }

	 public void setRole(USER_ROLE role) {
		 this.role = role;
	 }

	 public Set<Address> getAddresses() {
		 return addresses;
	 }

	 public void setAddresses(Set<Address> addresses) {
		 this.addresses = addresses;
	 }

	 public Set<Coupon> getUsedCoupons() {
		 return usedCoupons;
	 }

	 public void setUsedCoupons(Set<Coupon> usedCoupons) {
		 this.usedCoupons = usedCoupons;
	 }
	 
	 
	
	
	
}
