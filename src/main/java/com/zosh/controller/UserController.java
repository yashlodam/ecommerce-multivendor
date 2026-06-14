package com.zosh.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.zosh.model.User;
import com.zosh.service.UserService;

@RestController
public class UserController {

	@Autowired
	private UserService userService;
	
	@GetMapping("/users/profile")
	public ResponseEntity<User> getUserDetails(@RequestHeader("Authorization") String jwt){
		
		User user = userService.findUserByJwtToken(jwt);
		
		return ResponseEntity.ok(user);
	}
}
