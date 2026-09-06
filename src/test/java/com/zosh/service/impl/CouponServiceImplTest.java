package com.zosh.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zosh.exceptions.DuplicateResourceException;
import com.zosh.exceptions.ResourceNotFoundException;
import com.zosh.model.Cart;
import com.zosh.model.CartItem;
import com.zosh.model.Coupon;
import com.zosh.model.User;
import com.zosh.repository.CartRepository;
import com.zosh.repository.CouponRepository;
import com.zosh.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {

    @Mock private CouponRepository couponRepo;
    @Mock private CartRepository cartRepo;
    @Mock private UserRepository userRepo;

    @InjectMocks
    private CouponServiceImpl couponService;

    private User user;
    private Coupon validCoupon;
    private Cart cart;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("buyer@example.com");
        user.setUsedCoupons(new HashSet<>());

        validCoupon = new Coupon();
        validCoupon.setId(10L);
        validCoupon.setCode("SAVE20");
        validCoupon.setDiscountPercentage(20.0);
        validCoupon.setMinimumOrderValue(500.0);
        validCoupon.setActive(true);
        validCoupon.setValidityStartDate(LocalDate.now().minusDays(1));
        validCoupon.setValidityEndDate(LocalDate.now().plusDays(10));

        cart = new Cart();
        cart.setId(100L);
        cart.setUser(user);
        cart.setTotalSellingPrice(1000.0);

        CartItem item = new CartItem();
        item.setId(200L);
        item.setSellingPrice(1000);
        item.setQuantity(1);
        cart.setCartItems(Set.of(item));
    }

    @Test
    @DisplayName("applyCoupon — successfully applies 20% discount on valid cart")
    void applyCoupon_Success() {
        when(couponRepo.findByCode("SAVE20")).thenReturn(validCoupon);
        when(cartRepo.findByUserId(1L)).thenReturn(cart);
        when(cartRepo.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cart updatedCart = couponService.applyCoupon("SAVE20", 1000.0, user);

        assertNotNull(updatedCart);
        assertEquals(800.0, updatedCart.getTotalSellingPrice());
        assertEquals(200, updatedCart.getDiscount());
        assertEquals("SAVE20", updatedCart.getCouponCode());
        assertTrue(user.getUsedCoupons().contains(validCoupon));
        verify(userRepo, times(1)).save(user);
    }

    @Test
    @DisplayName("applyCoupon — throws ResourceNotFoundException when coupon code does not exist")
    void applyCoupon_NotFound() {
        when(couponRepo.findByCode("UNKNOWN")).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () ->
                couponService.applyCoupon("UNKNOWN", 1000.0, user));
    }

    @Test
    @DisplayName("applyCoupon — rejects inactive coupon")
    void applyCoupon_Inactive() {
        validCoupon.setActive(false);
        when(couponRepo.findByCode("SAVE20")).thenReturn(validCoupon);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                couponService.applyCoupon("SAVE20", 1000.0, user));
        assertTrue(ex.getMessage().contains("no longer active"));
    }

    @Test
    @DisplayName("applyCoupon — rejects expired coupon")
    void applyCoupon_Expired() {
        validCoupon.setValidityEndDate(LocalDate.now().minusDays(1));
        when(couponRepo.findByCode("SAVE20")).thenReturn(validCoupon);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                couponService.applyCoupon("SAVE20", 1000.0, user));
        assertTrue(ex.getMessage().contains("expired"));
    }

    @Test
    @DisplayName("applyCoupon — rejects if user already used the coupon")
    void applyCoupon_AlreadyUsed() {
        user.getUsedCoupons().add(validCoupon);
        when(couponRepo.findByCode("SAVE20")).thenReturn(validCoupon);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                couponService.applyCoupon("SAVE20", 1000.0, user));
        assertTrue(ex.getMessage().contains("already used"));
    }

    @Test
    @DisplayName("applyCoupon — rejects if cart total is below minimum order value")
    void applyCoupon_BelowMinimumOrderValue() {
        cart.getCartItems().iterator().next().setSellingPrice(300);
        cart.setTotalSellingPrice(300.0);

        when(couponRepo.findByCode("SAVE20")).thenReturn(validCoupon);
        when(cartRepo.findByUserId(1L)).thenReturn(cart);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                couponService.applyCoupon("SAVE20", 300.0, user));
        assertTrue(ex.getMessage().contains("Minimum order value"));
    }

    @Test
    @DisplayName("removeCoupon — restores original cart price and removes from user usedCoupons")
    void removeCoupon_Success() {
        user.getUsedCoupons().add(validCoupon);
        cart.setCouponCode("SAVE20");
        cart.setDiscount(200);
        cart.setTotalSellingPrice(800.0);

        when(couponRepo.findByCode("SAVE20")).thenReturn(validCoupon);
        when(cartRepo.findByUserId(1L)).thenReturn(cart);
        when(cartRepo.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cart restoredCart = couponService.removeCoupon("SAVE20", user);

        assertNotNull(restoredCart);
        assertEquals(1000.0, restoredCart.getTotalSellingPrice());
        assertEquals(0, restoredCart.getDiscount());
        assertNull(restoredCart.getCouponCode());
        assertFalse(user.getUsedCoupons().contains(validCoupon));
        verify(userRepo, times(1)).save(user);
    }

    @Test
    @DisplayName("createCoupon — throws DuplicateResourceException when coupon code already exists")
    void createCoupon_DuplicateCode() {
        when(couponRepo.findByCode("SAVE20")).thenReturn(validCoupon);

        assertThrows(DuplicateResourceException.class, () ->
                couponService.createCoupon(validCoupon));
    }
}
