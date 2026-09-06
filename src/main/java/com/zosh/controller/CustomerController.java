package com.zosh.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.zosh.model.Home;
import com.zosh.model.HomeCategory;
import com.zosh.response.CurrentUserResponse;
import com.zosh.response.HomeService;
import com.zosh.service.HomeCategoryService;
import com.zosh.service.UserService;

@RestController
public class CustomerController {

	@Autowired
	private HomeCategoryService homeCategoryService;
	
	@Autowired
	private HomeService homeService;
	
	@Autowired
	private UserService userService;
	
	@PostMapping("/home/categories")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Home> createHomeCategories(@RequestBody List<HomeCategory> homeCategories){
		List<HomeCategory> categories = homeCategoryService.createCategories(homeCategories);
		Home home = homeService.createHomePageData(categories);
		return new ResponseEntity<>(home,HttpStatus.ACCEPTED);
	}

	@GetMapping("/home/categories")
	public ResponseEntity<Home> getHomeCategories() {
		List<HomeCategory> categories = homeCategoryService.getAllHomeCategories();
		Home home = homeService.createHomePageData(categories);
		return new ResponseEntity<>(home, HttpStatus.OK);
	}
	
	@GetMapping("/auth/current-role")
	public CurrentUserResponse getCurrentRole(Authentication authentication) {
	    if (authentication == null || authentication.getAuthorities() == null || authentication.getAuthorities().isEmpty()) {
	        return new CurrentUserResponse("ROLE_CUSTOMER");
	    }
	    String role = authentication.getAuthorities()
	                                .iterator()
	                                .next()
	                                .getAuthority();

	    return new CurrentUserResponse(role);
	}
}
