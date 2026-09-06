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
		if (homeCategories == null || homeCategories.isEmpty()) {
			return homecategoryRepository.findAll();
		}

		List<HomeCategory> existing = homecategoryRepository.findAll();
		if (existing.isEmpty()) {
			return homecategoryRepository.saveAll(homeCategories);
		}

		java.util.Set<String> existingCategoryIds = existing.stream()
				.map(HomeCategory::getCategoryId)
				.filter(java.util.Objects::nonNull)
				.collect(java.util.stream.Collectors.toSet());

		List<HomeCategory> toSave = homeCategories.stream()
				.filter(c -> c.getCategoryId() != null && !existingCategoryIds.contains(c.getCategoryId()))
				.collect(java.util.stream.Collectors.toList());

		if (!toSave.isEmpty()) {
			homecategoryRepository.saveAll(toSave);
			return homecategoryRepository.findAll();
		}

		return existing;
	}

	@Override
	public HomeCategory updateHomeCategory(HomeCategory homeCategory, Long id) {
		
		HomeCategory existing = homecategoryRepository.findById(id)
				.orElseThrow(()-> new IllegalArgumentException("Category not found"));
		
		
		if(homeCategory.getName()!=null) {
			existing.setName(homeCategory.getName());
		}
		if(homeCategory.getImage()!=null) {
			existing.setImage(homeCategory.getImage());
		}
		if(homeCategory.getCategoryId()!=null) {
			existing.setCategoryId(homeCategory.getCategoryId());
		}
		if(homeCategory.getSection()!=null) {
			existing.setSection(homeCategory.getSection());
		}
		if(homeCategory.getPriority()!=null) {
			existing.setPriority(homeCategory.getPriority());
		}
		
		return homecategoryRepository.save(existing);
	}

	@Override
	public List<HomeCategory> getAllHomeCategories() {
		// TODO Auto-generated method stub
		return homecategoryRepository.findAll();
	}

}
