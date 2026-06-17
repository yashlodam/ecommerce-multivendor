package com.zosh.service.impl;

import org.springframework.beans.factory.annotation.Autowired;

import com.zosh.model.Product;
import com.zosh.model.User;
import com.zosh.model.wishlist;
import com.zosh.repository.wishlistRepository;
import com.zosh.service.WishlistService;

public class WishlistServiceImpl implements WishlistService {

	@Autowired
	private wishlistRepository wishlistrepo;
	
	
	@Override
	public wishlist createWishlist(User user) {
		
		wishlist w = new wishlist();
		
		w.setUser(user);
		
		return wishlistrepo.save(w);
	}

	@Override
	public wishlist getWishlistByUserId(User user) {
		
		wishlist w = wishlistrepo.findByUserId(user.getId());
		if(w==null) {
			w = createWishlist(user);
		}
		return w;
	}

	@Override
	public wishlist addProductToWishlist(User user, Product product) {

	    wishlist wishlist = getWishlistByUserId(user);

	    if (wishlist == null) {
	        wishlist = createWishlist(user);
	    }

	    if (wishlist.getProducts().contains(product)) {
	        wishlist.getProducts().remove(product);
	    } else {
	        wishlist.getProducts().add(product);
	    }

	    return wishlistrepo.save(wishlist);
	}
}
