package com.zosh.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.zosh.model.Home;
import com.zosh.model.HomeCategory;
import com.zosh.response.HomeService;
import com.zosh.service.HomeCategoryService;

@RestController

public class HomeCategoryController {

	@Autowired
	private HomeCategoryService homeCategoryService;
	
	@Autowired
	private HomeService homeService;
	
	
	@PostMapping("/home/categories")
	public ResponseEntity<Home> createHomeCategories(@RequestBody List<HomeCategory> homeCategories)
	{
		
		List<HomeCategory> categories = homeCategoryService.createCategories(homeCategories);
		
		Home home = homeService.createHomePageData(categories);
		
		return  new ResponseEntity<>(home,HttpStatus.ACCEPTED);
		
		
	}
	
	
	
	@GetMapping("/admin/home-category")
	public ResponseEntity<List<HomeCategory>> getHomeCategory(
			)
	{
		List<HomeCategory> categories = homeCategoryService.getAllHomeCategories();
		return ResponseEntity.ok(categories);
	}
	
	
	@PatchMapping("/admin/home-category/{id}")
	public ResponseEntity<HomeCategory> updateHomeCategory(
			@PathVariable Long id,
			@RequestBody HomeCategory homeCategory
			)
	{
		HomeCategory updateCategory = homeCategoryService.updateHomeCategory(homeCategory, id);
		
		return ResponseEntity.ok(updateCategory);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
