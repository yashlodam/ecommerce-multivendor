package com.zosh.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zosh.response.ApiResponse;

@RestController
public class HomeController {

	
	@GetMapping()
	public ApiResponse HomeControllerHandler() {
		
		ApiResponse apiResponse = new ApiResponse();
		
		apiResponse.setMessage("Welcome to ecommerce multi vendor system");
		
		return apiResponse;
	}
}

