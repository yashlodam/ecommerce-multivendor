package com.zosh.service;

import java.math.BigDecimal;

import com.zosh.dto.pricing.ProductPricingDto;
import com.zosh.model.Cart;
import com.zosh.model.CartItem;
import com.zosh.model.Deal;
import com.zosh.model.Product;
import com.zosh.model.ProductVariant;

public interface PricingService {

    /**
     * Calculates the real-time pricing breakdown for a product or variant,
     * applying the best eligible active promotional deal.
     */
    ProductPricingDto calculateProductPricing(Product product, ProductVariant variant);

    /**
     * Applies promotional deal pricing directly to a cart item.
     */
    ProductPricingDto applyDealPricingToCartItem(CartItem item);

    /**
     * Evaluates order-level deals against a cart subtotal.
     * Returns the best applicable order deal, or null if none qualify.
     */
    Deal findBestOrderDeal(BigDecimal cartSubtotal);

    /**
     * Calculates the order-level discount for a cart subtotal.
     */
    BigDecimal calculateOrderDealDiscount(BigDecimal cartSubtotal);
}
