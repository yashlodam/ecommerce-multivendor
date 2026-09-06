package com.zosh.service;

import com.zosh.model.Cart;
import com.zosh.model.CartItem;
import com.zosh.model.Product;
import com.zosh.model.ProductVariant;
import com.zosh.model.User;

public interface CartService {

	/** Legacy overload — no variant (backward compat). */
	CartItem addCartItem(User user, Product product, String size, int quantity);

	/**
	 * Variant-aware overload.
	 * When variant is non-null, pricing is sourced from the variant;
	 * otherwise falls back to product-level prices.
	 */
	CartItem addCartItem(User user, Product product, ProductVariant variant, String size, int quantity);

	Cart findUserCart(User user);

	void clearCart(User user);
}
