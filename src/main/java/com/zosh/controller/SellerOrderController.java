package com.zosh.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zosh.domain.OrderStatus;
import com.zosh.model.Order;
import com.zosh.model.Seller;
import com.zosh.service.OrderService;
import com.zosh.service.SellerService;

@RestController
@RequestMapping("/api/seller/orders")
public class SellerOrderController {

	
	@Autowired
	private OrderService orderService;
	
	@Autowired
	private SellerService sellerService;
	
	
	
	@GetMapping()
	public ResponseEntity<List<Order>> getAllOrderHandler(
			@RequestHeader("Authorization") String jwt
			)
	{
		Seller seller = sellerService.getSellerProfile(jwt);
		
		List<Order> orders = orderService.sellersOrder(seller.getId());
		
		return new ResponseEntity<>(orders,HttpStatus.ACCEPTED);
	}
	
	
	@PatchMapping("/{orderId}/status/{orderStatus}")
	public ResponseEntity<Order> updateOrderHandler(
			@RequestHeader("Authorization") String jwt,
			@PathVariable Long orderId,
			@PathVariable OrderStatus orderStatus
			)
	{
		
		
		Order order = orderService.updateOrderStatus(orderId, orderStatus);
		
		return new ResponseEntity<>(order,HttpStatus.ACCEPTED);
	}
	
	
	
	
	
	
	
}
