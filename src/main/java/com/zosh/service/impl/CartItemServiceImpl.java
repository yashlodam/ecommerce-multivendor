package com.zosh.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zosh.model.CartItem;
import com.zosh.model.User;
import com.zosh.repository.CartItemRepository;
import com.zosh.service.CartItemService;

@Service
public class CartItemServiceImpl implements CartItemService{

	@Autowired
	private CartItemRepository cartItemRepository;
	
	@Override
	public CartItem updateCartItem(Long userId, Long id, CartItem cartItem) {
		
		CartItem item = findCartItemById(id);
		
		User cartItemUser = item.getCart().getUser();
		
		if(cartItemUser.getId().equals(userId)) {
			
			item.setQuantity(cartItem.getQuantity());
			item.setMrpPrice(item.getQuantity()*item.getProduct().getMrpPrice());
			item.setSellingPrice(item.getQuantity()*item.getProduct().getSellingPrice());
			
			return cartItemRepository.save(item);
		}
		
		throw new RuntimeException("You can't update this cartItem");
	}

	@Override
	public void removeCartItem(Long userId, Long cartItemId) {
        CartItem item = findCartItemById(cartItemId);
		
		User cartItemUser = item.getCart().getUser();
		
		if(cartItemUser.getId().equals(userId)) {
			cartItemRepository.delete(item);
		}
		
		else throw new RuntimeException("you can't delete this item");
		
	}

	@Override
	public CartItem findCartItemById(Long id) {
		
		return cartItemRepository.findById(id).orElseThrow(()-> new RuntimeException("cart item not found with id"));
	}

}
