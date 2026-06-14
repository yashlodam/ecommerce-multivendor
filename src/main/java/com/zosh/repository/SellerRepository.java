package com.zosh.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zosh.model.Seller;

public interface SellerRepository  extends JpaRepository<Seller, Long>{

	
	Seller findByEmail(String email);
	
}
