package com.zosh.service;

import java.util.List;

import com.zosh.model.Cart;
import com.zosh.model.Coupon;
import com.zosh.model.User;

public interface CouponService {

	Cart applyCoupon(String code,double orderValue,User user);
	
	Cart removeCoupon(String code,User user);
	Coupon findCouponById(Long id);
	
	Coupon createCoupon(Coupon coupon);
	
	List<Coupon> findAllCoupons();
	
	void deleteCoupon(Long id);
}
