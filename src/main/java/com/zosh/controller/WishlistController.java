package com.zosh.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zosh.model.Product;
import com.zosh.model.User;
import com.zosh.model.wishlist;
import com.zosh.service.ProductService;
import com.zosh.service.UserService;
import com.zosh.service.WishlistService;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

	@Autowired
	private WishlistService wishlistService;
	@Autowired
	private UserService userService;
	
	@Autowired
	private ProductService productService;
	
	
	
	@GetMapping()
	public ResponseEntity<wishlist> getWishlistByUserId(
			@RequestHeader("Authorization") String jwt
			)
	{
		User user = userService.findUserByJwtToken(jwt);
		wishlist w = wishlistService.getWishlistByUserId(user);
		
		return ResponseEntity.ok(w);
		
	}
	
	
	@PostMapping("/add-product/{productId}")
	public ResponseEntity<wishlist> addProductToWishlist(
			@PathVariable Long productId,
			@RequestHeader("Authorization") String jwt
			)
	{
		Product product = productService.findProductById(productId);
		
		User user = userService.findUserByJwtToken(jwt);
		
		wishlist w = wishlistService.addProductToWishlist(user, product);
		
		return new ResponseEntity<>(w,HttpStatus.CREATED);
		
	}
	
	
	
	
	
	
	
	
	
	
	
}
