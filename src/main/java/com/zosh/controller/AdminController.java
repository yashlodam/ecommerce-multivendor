package com.zosh.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zosh.domain.AccountStatus;
import com.zosh.exceptions.SellerException;
import com.zosh.model.Seller;
import com.zosh.model.User;
import com.zosh.service.SellerService;
import com.zosh.service.UserService;

@RestController
@RequestMapping("/api/")
public class AdminController {

	@Autowired
	private SellerService sellerService;
	
	@Autowired
	private UserService userService;
	
	
	@PatchMapping("/seller/{id}/status/{status}")
	public ResponseEntity<Seller> updateSellerStatus(
			@PathVariable Long id,
			@PathVariable AccountStatus status
			) throws SellerException
	{
		
		Seller updateSeller = sellerService.updateSellerAccountStatus(id, status);
		
		return ResponseEntity.ok(updateSeller);
		
	}
	
	
	@PutMapping("/admin/users/{userId}/ban")
	public ResponseEntity<User> banUser(
	        @PathVariable Long userId) {

	    User user = userService.banUser(userId);

	    return ResponseEntity.ok(user);
	}
	
}
