package com.zosh.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.zosh.model.Cart;
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
	public Cart applyCoupon(String code, double orderValue, User user) {
		
		Coupon coupon = couponRepository.findByCode(code);
		
		Cart cart = cartRepository.findByUserId(user.getId());
		
		if(coupon==null) {
			throw new RuntimeException("Coupon not valid");
		}
		if(user.getUsedCoupons().contains(coupon)) {
			throw new RuntimeException("Coupon already used");
		}
		
		if(orderValue<coupon.getMinimumOrderValue()) {
			throw new RuntimeException("valid for  minimum order value"+coupon.getMinimumOrderValue());
		}
		
		if(coupon.isActive() && LocalDate.now().isAfter(coupon.getValidityStartDate()) && LocalDate.now().isBefore(coupon.getValidityEndDate())) {
			
			user.getUsedCoupons().add(coupon);
			userRepository.save(user);
			
			double discountedPrice = (cart.getTotalSellingPrice()*coupon.getDiscountPercentage())/100;
			
			cart.setTotalSellingPrice(cart.getTotalSellingPrice()-discountedPrice);
			cart.setCouponCode(code);
			cartRepository.save(cart);
			
			return cart;
		}
		
        throw new RuntimeException("coupon not valid...");
	}

	@Override
	public Cart removeCoupon(String code, User user) {
		
		Coupon coupon = couponRepository.findByCode(code);
		
		if(coupon==null) {
			throw new RuntimeException("coupon not valid..");
		}
		Cart cart = cartRepository.findByUserId(user.getId());
		
		double discountedPrice = (cart.getTotalSellingPrice()*coupon.getDiscountPercentage())/100;
		
		cart.setTotalSellingPrice(cart.getTotalSellingPrice()+discountedPrice);
		
		cart.setCouponCode(null);
		
		
		
		return cartRepository.save(cart);
	}

	@Override
	public Coupon findCouponById(Long id) {
		
		return couponRepository.findById(id).orElseThrow(()-> new IllegalArgumentException("Coupon not found with id..."));
	}

	@Override
	@PreAuthorize("hasRole ('ADMIN')")
	public Coupon createCoupon(Coupon coupon) {
		
		return couponRepository.save(coupon);
	}

	@Override
	public List<Coupon> findAllCoupons() {
		// TODO Auto-generated method stub
		return couponRepository.findAll();
	}

	@Override
	@PreAuthorize("hasRole ('ADMIN')")
	public void deleteCoupon(Long id) {
		
		findCouponById(id);
		
		couponRepository.deleteById(id);
		
	}

}
