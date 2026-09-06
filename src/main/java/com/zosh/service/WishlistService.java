package com.zosh.service;

import com.zosh.model.Product;
import com.zosh.model.User;
import com.zosh.model.Wishlist;

public interface WishlistService {

    Wishlist createWishlist(User user);

    Wishlist getWishlistByUserId(User user);

    Wishlist addProductToWishlist(User user, Product product);

    Wishlist removeProductFromWishlist(User user, Long productId);
}
