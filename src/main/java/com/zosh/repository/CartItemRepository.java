package com.zosh.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import com.zosh.model.Cart;
import com.zosh.model.CartItem;
import com.zosh.model.Product;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

	CartItem findByCartAndProductAndSize(Cart cart, Product product,String size);

	@Modifying
	@Transactional
	void deleteByProductId(Long productId);

	@Modifying
	@Transactional
	void deleteByCart(Cart cart);
	
}
