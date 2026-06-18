package com.zosh.service;

import java.util.List;

import com.zosh.model.HomeCategory;

public interface HomeCategoryService {

	HomeCategory createHomeCategory(HomeCategory homecategory);
	List<HomeCategory> createCategories(List<HomeCategory> homeCategories);
	HomeCategory updateHomeCategory(HomeCategory homeCategory,Long id);
	List<HomeCategory> getAllHomeCategories();
}
