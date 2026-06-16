package com.zosh.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zosh.model.Cart;
import com.zosh.model.CartItem;
import com.zosh.model.Product;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

	CartItem findByCartAndProductAndSize(Cart cart, Product product,String size);
	
}
