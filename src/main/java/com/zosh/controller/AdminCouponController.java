package com.zosh.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
import com.zosh.service.CartService;
import com.zosh.service.CouponService;
import com.zosh.service.UserService;

@RestController
@RequestMapping("/api/coupons")
public class AdminCouponController {

	@Autowired
	private CouponService couponService;
	
	@Autowired
	private UserService service;
	
	@Autowired
	private CartService cartService;
	
	
	
	@PostMapping("/apply")
	public ResponseEntity<Cart> applyCoupon(
			@RequestParam String apply,
			@RequestParam String code,
			@RequestParam double orderValue,
			@RequestHeader("Authorization") String jwt
			)
	{
		
		
		User user = service.findUserByJwtToken(jwt);
		
		Cart cart;
		
		if(apply.equals("true")) {
			cart = couponService.applyCoupon(code, orderValue, user);
		} 
		else {
			cart = couponService.removeCoupon(code, user);
		}
		
		return ResponseEntity.ok(cart);
		
	}
	
	
	
	// Admin oepration
	
	@PostMapping("/admin/create")
	public ResponseEntity<Coupon> createCoupon(@RequestBody Coupon coupon){
		Coupon createdCoupon = couponService.createCoupon(coupon);
		
		return ResponseEntity.ok(createdCoupon);
	}
	
	@DeleteMapping("/admin/delete/{id}")
	public ResponseEntity<String> deleteCoupon(@PathVariable Long id){
		couponService.deleteCoupon(id);
		return ResponseEntity.ok("Coupon deleted successfulyy");
	}
	
	@GetMapping("/admin/all")
	public ResponseEntity<List<Coupon>> getAllCoupons(){
		
		List<Coupon> coupons = couponService.findAllCoupons();
		
		return ResponseEntity.ok(coupons);
	}
	
	
	
	
	
	
	
	
	
	
	
}
