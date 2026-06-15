package com.zosh.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zosh.domain.AccountStatus;
import com.zosh.model.Seller;

public interface SellerRepository  extends JpaRepository<Seller, Long>{

	
	Seller findByEmail(String email);
	
	

	List<Seller> findByAccountStatus(AccountStatus status);
	
	
	
}
