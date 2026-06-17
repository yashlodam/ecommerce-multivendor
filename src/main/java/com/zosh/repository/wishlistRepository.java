package com.zosh.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zosh.model.wishlist;

public interface wishlistRepository extends JpaRepository<wishlist, Long>{

	wishlist findByUserId(Long userId);
	
}
