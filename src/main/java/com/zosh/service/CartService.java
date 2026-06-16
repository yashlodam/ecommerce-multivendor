package com.zosh.service;

import com.zosh.model.Cart;
import com.zosh.model.CartItem;
import com.zosh.model.Product;
import com.zosh.model.User;

public interface CartService {

	CartItem addCartItem(User user,Product product,String size,int quantity);
	
	Cart findUserCart(User user);
}
