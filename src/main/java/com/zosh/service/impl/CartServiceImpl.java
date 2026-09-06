package com.zosh.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zosh.dto.pricing.ProductPricingDto;
import com.zosh.model.Cart;
import com.zosh.model.CartItem;
import com.zosh.model.Coupon;
import com.zosh.model.Product;
import com.zosh.model.ProductVariant;
import com.zosh.model.User;
import com.zosh.repository.CartItemRepository;
import com.zosh.repository.CartRepository;
import com.zosh.repository.CouponRepository;
import com.zosh.service.CartService;
import com.zosh.service.PricingService;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private PricingService pricingService;

    @Autowired
    private CouponRepository couponRepository;

    // ─── Legacy overload (backward compat) ─────────────────────────────────────

    @Override
    public CartItem addCartItem(User user, Product product, String size, int quantity) {
        return addCartItem(user, product, null, size, quantity);
    }

    // ─── Variant-aware overload with Deal Pricing ──────────────────────────────

    @Override
    @Transactional
    public CartItem addCartItem(User user, Product product, ProductVariant variant, String size, int quantity) {
        Cart cart = findUserCart(user);

        // Evaluate live promotional deal pricing
        ProductPricingDto pricing = pricingService.calculateProductPricing(product, variant);
        int unitMrp = pricing.getMrpPrice();
        int unitSelling = pricing.getEffectivePrice();

        String sizeKey = (size != null && !size.isBlank()) ? size
                          : (variant != null ? variant.getVariantName() : "Standard");

        CartItem existingItem = cartItemRepository.findByCartAndProductAndSize(cart, product, sizeKey);

        if (existingItem != null) {
            // Item already in cart — bump quantity and recalculate totals
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            existingItem.setMrpPrice(existingItem.getQuantity() * unitMrp);
            existingItem.setSellingPrice(existingItem.getQuantity() * unitSelling);

            CartItem saved = cartItemRepository.save(existingItem);
            findUserCart(user); // recalculate cart totals
            return saved;
        }

        // Create a new CartItem
        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setVariant(variant);
        cartItem.setUserId(user.getId());
        cartItem.setQuantity(quantity);
        cartItem.setSize(sizeKey);
        cartItem.setMrpPrice(quantity * unitMrp);
        cartItem.setSellingPrice(quantity * unitSelling);

        CartItem saved = cartItemRepository.save(cartItem);
        cart.getCartItems().add(saved);
        findUserCart(user); // recalculate cart totals

        return saved;
    }

    @Override
    @Transactional
    public Cart findUserCart(User user) {

        Cart cart = cartRepository.findByUserId(user.getId());

        if (cart == null) {
            cart = new Cart();
            cart.setUser(user);
            cart = cartRepository.save(cart);
        }

        int totalMrpPrice = 0;
        int totalSellingPrice = 0;
        int totalItems = 0;

        for (CartItem cartItem : cart.getCartItems()) {
            pricingService.applyDealPricingToCartItem(cartItem);
            cartItemRepository.save(cartItem);

            totalMrpPrice += (cartItem.getMrpPrice() != null ? cartItem.getMrpPrice() : 0);
            totalSellingPrice += (cartItem.getSellingPrice() != null ? cartItem.getSellingPrice() : 0);
            totalItems += cartItem.getQuantity();
        }

        // Recalculate coupon discount or fallback to order-level deals
        int finalSellingPrice = totalSellingPrice;
        int couponDiscount = 0;

        if (cart.getCouponCode() != null && !cart.getCouponCode().isBlank()) {
            Coupon coupon = couponRepository.findByCode(cart.getCouponCode().trim().toUpperCase());
            LocalDate today = LocalDate.now();
            boolean isValidCoupon = coupon != null && coupon.isActive()
                    && (coupon.getValidityStartDate() == null || !today.isBefore(coupon.getValidityStartDate()))
                    && (coupon.getValidityEndDate() == null || !today.isAfter(coupon.getValidityEndDate()));

            if (isValidCoupon && totalSellingPrice >= coupon.getMinimumOrderValue()) {
                couponDiscount = (int) Math.round((totalSellingPrice * coupon.getDiscountPercentage()) / 100.0);
                finalSellingPrice = Math.max(0, totalSellingPrice - couponDiscount);
            } else {
                // Cart no longer qualifies or coupon expired
                cart.setCouponCode(null);
            }
        } else {
            BigDecimal orderDealDiscount = pricingService.calculateOrderDealDiscount(BigDecimal.valueOf(totalSellingPrice));
            finalSellingPrice = Math.max(0, totalSellingPrice - orderDealDiscount.intValue());
        }

        cart.setTotalMrpPrice(totalMrpPrice);
        cart.setTotalSellingPrice(finalSellingPrice);
        cart.setTotalItem(totalItems);
        cart.setDiscount(couponDiscount);

        cartRepository.save(cart);

        return cart;
    }

    private int calculateDiscountPercentage(int mrpPrice, int sellingPrice) {
        if (mrpPrice <= 0) {
            return 0;
        }

        double discount = mrpPrice - sellingPrice;
        double discountPercentage = (discount / mrpPrice) * 100;

        return (int) Math.round(discountPercentage);
    }

    @Override
    @Transactional
    public void clearCart(User user) {

        Cart cart = findUserCart(user);

        cartItemRepository.deleteAll(cart.getCartItems());
        cart.getCartItems().clear();

        cart.setTotalItem(0);
        cart.setTotalMrpPrice(0);
        cart.setTotalSellingPrice(0);
        cart.setDiscount(0);
        cart.setCouponCode(null);

        cartRepository.save(cart);
    }
}