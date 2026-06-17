package com.zosh.service;

import com.zosh.model.Product;
import com.zosh.model.User;
import com.zosh.model.wishlist;

public interface WishlistService {

	wishlist createWishlist(User user);
	
	wishlist getWishlistByUserId(User user);
	
	wishlist addProductToWishlist(User user, Product product);
	
	
}
