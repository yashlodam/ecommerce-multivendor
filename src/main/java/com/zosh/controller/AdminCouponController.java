package com.zosh.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zosh.model.Cart;
import com.zosh.model.Coupon;
import com.zosh.model.User;
import com.zosh.request.CouponRequest;
import com.zosh.response.ApiResponse;
import com.zosh.service.CouponService;
import com.zosh.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/coupons")
@Tag(name = "Coupons", description = "Coupon validation, application, and administrative management endpoints")
public class AdminCouponController {

    @Autowired
    private CouponService couponService;

    @Autowired
    private UserService userService;

    @PostMapping("/apply")
    @Operation(summary = "Apply or remove a coupon on the customer's cart")
    public ResponseEntity<Cart> applyCoupon(
            @RequestParam String apply,
            @RequestParam String code,
            @RequestParam double orderValue,
            @RequestHeader("Authorization") String jwt) {

        User user = userService.findUserByJwtToken(jwt);
        Cart cart;

        if ("true".equalsIgnoreCase(apply)) {
            cart = couponService.applyCoupon(code, orderValue, user);
        } else {
            cart = couponService.removeCoupon(code, user);
        }

        return ResponseEntity.ok(cart);
    }

    @GetMapping("/active")
    @Operation(summary = "Get all currently active coupons within validity period")
    public ResponseEntity<List<Coupon>> getActiveCoupons() {
        List<Coupon> activeCoupons = couponService.findActiveCoupons();
        return ResponseEntity.ok(activeCoupons);
    }

    // ─── ADMIN OPERATIONS ────────────────────────────────────────────────────

    @PostMapping("/admin/create")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new coupon (Admin only)")
    public ResponseEntity<Coupon> createCoupon(@Valid @RequestBody CouponRequest req) {
        Coupon coupon = new Coupon();
        coupon.setCode(req.getCode().trim().toUpperCase());
        coupon.setDiscountPercentage(req.getDiscountPercentage());
        coupon.setValidityStartDate(req.getValidityStartDate());
        coupon.setValidityEndDate(req.getValidityEndDate());
        coupon.setMinimumOrderValue(req.getMinimumOrderValue());
        coupon.setActive(req.isActive());

        Coupon createdCoupon = couponService.createCoupon(coupon);
        return new ResponseEntity<>(createdCoupon, HttpStatus.CREATED);
    }

    @DeleteMapping("/admin/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a coupon by ID (Admin only)")
    public ResponseEntity<ApiResponse> deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        ApiResponse res = new ApiResponse("Coupon deleted successfully");
        return ResponseEntity.ok(res);
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all coupons (Admin only)")
    public ResponseEntity<List<Coupon>> getAllCoupons() {
        List<Coupon> coupons = couponService.findAllCoupons();
        return ResponseEntity.ok(coupons);
    }
}
