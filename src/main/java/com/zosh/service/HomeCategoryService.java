package com.zosh.service;

import java.util.List;

import com.zosh.domain.HomeCategorySection;
import com.zosh.model.HomeCategory;

public interface HomeCategoryService {

	HomeCategory createHomeCategory(HomeCategory homeCategory);
	List<HomeCategory> createCategories(List<HomeCategory> homeCategories);
	HomeCategory updateHomeCategory(HomeCategory homeCategory, Long id);
	List<HomeCategory> getAllHomeCategories();
	List<HomeCategory> getHomeCategoriesBySection(HomeCategorySection section);
	HomeCategory getHomeCategoryById(Long id);
	void deleteHomeCategory(Long id);
}
