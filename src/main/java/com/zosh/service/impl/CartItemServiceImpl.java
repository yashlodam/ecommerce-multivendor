package com.zosh.service.impl;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zosh.exceptions.ResourceNotFoundException;
import com.zosh.model.Cart;
import com.zosh.model.CartItem;
import com.zosh.model.User;
import com.zosh.repository.CartItemRepository;
import com.zosh.repository.CartRepository;
import com.zosh.service.CartItemService;
import com.zosh.service.CartService;
import com.zosh.service.PricingService;

@Service
public class CartItemServiceImpl implements CartItemService {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    @Lazy
    private CartService cartService;

    @Autowired
    private PricingService pricingService;

    @Override
    @Transactional
    public CartItem updateCartItem(Long userId, Long id, CartItem cartItem) {

        CartItem item = findCartItemById(id);
        Cart cart = item.getCart();
        User cartItemUser = cart != null ? cart.getUser() : null;

        if (cartItemUser == null || !cartItemUser.getId().equals(userId)) {
            throw new AccessDeniedException("You cannot update an item in another user's cart.");
        }

        item.setQuantity(cartItem.getQuantity());

        // Apply deal pricing
        pricingService.applyDealPricingToCartItem(item);
        CartItem saved = cartItemRepository.save(item);

        // Recalculate totals directly on the managed Cart entity
        int totalMrp = 0;
        int totalSelling = 0;
        int totalItems = 0;
        for (CartItem ci : cart.getCartItems()) {
            pricingService.applyDealPricingToCartItem(ci);
            totalMrp += (ci.getMrpPrice() != null ? ci.getMrpPrice() : 0);
            totalSelling += (ci.getSellingPrice() != null ? ci.getSellingPrice() : 0);
            totalItems += ci.getQuantity();
        }

        int finalSelling = totalSelling;
        if (cart.getCouponCode() == null || cart.getCouponCode().isBlank()) {
            BigDecimal orderDealDiscount = pricingService.calculateOrderDealDiscount(BigDecimal.valueOf(totalSelling));
            finalSelling = Math.max(0, totalSelling - orderDealDiscount.intValue());
        }

        cart.setTotalMrpPrice(totalMrp);
        cart.setTotalSellingPrice(finalSelling);
        cart.setTotalItem(totalItems);
        cart.setDiscount(totalMrp > 0 ? (int) Math.round(((double) (totalMrp - finalSelling) / totalMrp) * 100) : 0);

        cartRepository.save(cart);
        return saved;
    }

    @Override
    @Transactional
    public void removeCartItem(Long userId, Long cartItemId) {
        CartItem item = findCartItemById(cartItemId);
        Cart cart = item.getCart();
        User cartItemUser = cart != null ? cart.getUser() : null;

        if (cartItemUser == null || !cartItemUser.getId().equals(userId)) {
            throw new AccessDeniedException("You cannot delete an item from another user's cart.");
        }

        // Unlink from the parent Cart's collection to prevent cascade issues
        cart.getCartItems().removeIf(ci -> ci.getId() != null && ci.getId().equals(cartItemId));

        // Delete the item entity
        cartItemRepository.delete(item);

        // Recalculate totals directly on the managed Cart entity
        int totalMrp = 0;
        int totalSelling = 0;
        int totalItems = 0;
        for (CartItem ci : cart.getCartItems()) {
            if (ci.getId() != null && !ci.getId().equals(cartItemId)) {
                pricingService.applyDealPricingToCartItem(ci);
                totalMrp += (ci.getMrpPrice() != null ? ci.getMrpPrice() : 0);
                totalSelling += (ci.getSellingPrice() != null ? ci.getSellingPrice() : 0);
                totalItems += ci.getQuantity();
            }
        }

        int finalSelling = totalSelling;
        if (cart.getCouponCode() == null || cart.getCouponCode().isBlank()) {
            BigDecimal orderDealDiscount = pricingService.calculateOrderDealDiscount(BigDecimal.valueOf(totalSelling));
            finalSelling = Math.max(0, totalSelling - orderDealDiscount.intValue());
        }

        cart.setTotalMrpPrice(totalMrp);
        cart.setTotalSellingPrice(finalSelling);
        cart.setTotalItem(totalItems);
        cart.setDiscount(totalMrp > 0 ? (int) Math.round(((double) (totalMrp - finalSelling) / totalMrp) * 100) : 0);

        cartRepository.save(cart);
    }

    @Override
    public CartItem findCartItemById(Long id) {
        return cartItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", id));
    }
}
