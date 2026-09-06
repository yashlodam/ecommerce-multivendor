package com.zosh.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zosh.exceptions.DuplicateResourceException;
import com.zosh.exceptions.ResourceNotFoundException;
import com.zosh.model.Cart;
import com.zosh.model.CartItem;
import com.zosh.model.Coupon;
import com.zosh.model.User;
import com.zosh.repository.CartRepository;
import com.zosh.repository.CouponRepository;
import com.zosh.repository.UserRepository;
import com.zosh.service.CouponService;

@Service
public class CouponServiceImpl implements CouponService {

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public Cart applyCoupon(String code, double orderValue, User user) {
        Coupon coupon = couponRepository.findByCode(code.trim().toUpperCase());

        if (coupon == null) {
            throw new ResourceNotFoundException("Coupon", "code", code);
        }

        if (!coupon.isActive()) {
            throw new IllegalArgumentException("Coupon '" + code + "' is no longer active.");
        }

        LocalDate today = LocalDate.now();
        if (today.isBefore(coupon.getValidityStartDate()) || today.isAfter(coupon.getValidityEndDate())) {
            throw new IllegalArgumentException("Coupon '" + code + "' has expired or is not yet valid.");
        }

        User managedUser = userRepository.findById(user.getId())
                .orElse(user);

        if (managedUser.getUsedCoupons() != null && managedUser.getUsedCoupons().contains(coupon)) {
            throw new IllegalArgumentException("You have already used coupon '" + code + "'.");
        }

        Cart cart = cartRepository.findByUserId(user.getId());
        if (cart == null || cart.getCartItems().isEmpty()) {
            throw new IllegalArgumentException("Your cart is empty.");
        }

        // Recalculate original subtotal from cart items
        double originalTotal = cart.getCartItems().stream()
                .mapToDouble(item -> (double) item.getSellingPrice())
                .sum();

        if (originalTotal < coupon.getMinimumOrderValue()) {
            throw new IllegalArgumentException("Minimum order value of ₹" + coupon.getMinimumOrderValue()
                    + " is required to apply coupon '" + code + "'. Current total: ₹" + originalTotal);
        }

        double discountAmount = (originalTotal * coupon.getDiscountPercentage()) / 100.0;
        double finalPrice = Math.max(0, originalTotal - discountAmount);

        cart.setTotalSellingPrice(finalPrice);
        cart.setDiscount((int) discountAmount);
        cart.setCouponCode(coupon.getCode());

        if (managedUser.getUsedCoupons() != null) {
            managedUser.getUsedCoupons().add(coupon);
            userRepository.save(managedUser);
        }

        return cartRepository.save(cart);
    }

    @Override
    @Transactional
    public Cart removeCoupon(String code, User user) {
        Coupon coupon = couponRepository.findByCode(code.trim().toUpperCase());
        if (coupon == null) {
            throw new ResourceNotFoundException("Coupon", "code", code);
        }

        Cart cart = cartRepository.findByUserId(user.getId());
        if (cart == null) {
            throw new ResourceNotFoundException("Cart for user", "userId", user.getId());
        }

        // Restore original total from cart items
        double originalTotal = cart.getCartItems().stream()
                .mapToDouble(item -> (double) item.getSellingPrice())
                .sum();

        cart.setTotalSellingPrice(originalTotal);
        cart.setDiscount(0);
        cart.setCouponCode(null);

        User managedUser = userRepository.findById(user.getId())
                .orElse(user);
        if (managedUser.getUsedCoupons() != null) {
            managedUser.getUsedCoupons().remove(coupon);
            userRepository.save(managedUser);
        }

        return cartRepository.save(cart);
    }

    @Override
    public Coupon findCouponById(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Coupon createCoupon(Coupon coupon) {
        if (couponRepository.findByCode(coupon.getCode().trim().toUpperCase()) != null) {
            throw new DuplicateResourceException("Coupon code '" + coupon.getCode() + "' already exists.");
        }
        coupon.setCode(coupon.getCode().trim().toUpperCase());
        return couponRepository.save(coupon);
    }

    @Override
    public List<Coupon> findAllCoupons() {
        return couponRepository.findAll();
    }

    @Override
    public List<Coupon> findActiveCoupons() {
        LocalDate today = LocalDate.now();
        return couponRepository.findAll().stream()
                .filter(c -> c.isActive()
                        && (c.getValidityStartDate() == null || !today.isBefore(c.getValidityStartDate()))
                        && (c.getValidityEndDate() == null || !today.isAfter(c.getValidityEndDate())))
                .toList();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteCoupon(Long id) {
        Coupon coupon = findCouponById(id);
        couponRepository.delete(coupon);
    }
}
