package com.zosh.service;

import com.zosh.model.CartItem;

public interface CartItemService {

	CartItem updateCartItem(Long userId,Long id,CartItem cartItem);
	
	void removeCartItem(Long userId,Long cartItemId);
	
	CartItem findCartItemById(Long id);
	
}
