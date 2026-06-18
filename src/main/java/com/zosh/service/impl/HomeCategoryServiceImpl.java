package com.zosh.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zosh.model.HomeCategory;
import com.zosh.repository.HomeCategoryRepository;
import com.zosh.service.HomeCategoryService;

@Service
public class HomeCategoryServiceImpl implements HomeCategoryService{
	
	@Autowired
	private HomeCategoryRepository homecategoryRepository;
	

	@Override
	public HomeCategory createHomeCategory(HomeCategory homecategory) {
		
		return homecategoryRepository.save(homecategory);
	}

	@Override
	public List<HomeCategory> createCategories(List<HomeCategory> homeCategories) {
		
		if(homecategoryRepository.findAll().isEmpty()) {
			
			return homecategoryRepository.saveAll(homeCategories);
		}
		
		return homecategoryRepository.findAll();
	}

	@Override
	public HomeCategory updateHomeCategory(HomeCategory homeCategory, Long id) {
		
		HomeCategory existing = homecategoryRepository.findById(id)
				.orElseThrow(()-> new IllegalArgumentException("Category not found"));
		
		
		if(homeCategory.getImage()!=null) {
			existing.setImage(homeCategory.getImage());;
		}
		
		if(homeCategory.getCategoryId()!=null) {
			existing.setCategoryId(homeCategory.getCategoryId());
		}
		
		return homecategoryRepository.save(existing);
	}

	@Override
	public List<HomeCategory> getAllHomeCategories() {
		// TODO Auto-generated method stub
		return homecategoryRepository.findAll();
	}

}
