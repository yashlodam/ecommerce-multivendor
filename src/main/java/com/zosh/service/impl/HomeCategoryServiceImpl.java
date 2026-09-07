package com.zosh.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zosh.domain.HomeCategorySection;
import com.zosh.model.Deal;
import com.zosh.model.HomeCategory;
import com.zosh.repository.DealRepository;
import com.zosh.repository.HomeCategoryRepository;
import com.zosh.service.HomeCategoryService;

@Service
public class HomeCategoryServiceImpl implements HomeCategoryService {
	
	@Autowired
	private HomeCategoryRepository homecategoryRepository;

	@Autowired(required = false)
	private DealRepository dealRepository;

	@Override
	@Transactional
	public HomeCategory createHomeCategory(HomeCategory homeCategory) {
		if (homeCategory.getName() == null || homeCategory.getName().trim().isEmpty()) {
			throw new IllegalArgumentException("Category name is required");
		}

		// Ensure category slug / identifier is populated
		if (homeCategory.getCategoryId() == null || homeCategory.getCategoryId().trim().isEmpty()) {
			String generatedSlug = homeCategory.getName().trim().toLowerCase().replaceAll("[^a-z0-9]+", "_");
			if (generatedSlug.endsWith("_")) {
				generatedSlug = generatedSlug.substring(0, generatedSlug.length() - 1);
			}
			homeCategory.setCategoryId(generatedSlug);
		} else {
			homeCategory.setCategoryId(homeCategory.getCategoryId().trim());
		}

		if (homeCategory.getSection() == null) {
			homeCategory.setSection(HomeCategorySection.SHOP_BY_CATEGORIES);
		}

		if (homeCategory.getPriority() == null) {
			homeCategory.setPriority(0);
		}
		
		return homecategoryRepository.save(homeCategory);
	}

	@Override
	@Transactional
	public List<HomeCategory> createCategories(List<HomeCategory> homeCategories) {
		if (homeCategories == null || homeCategories.isEmpty()) {
			return homecategoryRepository.findAllByOrderByPriorityAscIdAsc();
		}

		List<HomeCategory> existing = homecategoryRepository.findAll();
		if (existing.isEmpty()) {
			return homecategoryRepository.saveAll(homeCategories);
		}

		Set<String> existingCategoryIds = existing.stream()
				.map(HomeCategory::getCategoryId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		List<HomeCategory> toSave = homeCategories.stream()
				.filter(c -> c.getCategoryId() != null && !existingCategoryIds.contains(c.getCategoryId()))
				.collect(Collectors.toList());

		if (!toSave.isEmpty()) {
			homecategoryRepository.saveAll(toSave);
		}

		return homecategoryRepository.findAllByOrderByPriorityAscIdAsc();
	}

	@Override
	@Transactional
	public HomeCategory updateHomeCategory(HomeCategory homeCategory, Long id) {
		HomeCategory existing = homecategoryRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Home category not found with ID: " + id));
		
		if (homeCategory.getName() != null && !homeCategory.getName().trim().isEmpty()) {
			existing.setName(homeCategory.getName().trim());
		}
		if (homeCategory.getImage() != null) {
			existing.setImage(homeCategory.getImage().trim());
		}
		if (homeCategory.getCategoryId() != null && !homeCategory.getCategoryId().trim().isEmpty()) {
			existing.setCategoryId(homeCategory.getCategoryId().trim());
		}
		if (homeCategory.getSection() != null) {
			existing.setSection(homeCategory.getSection());
		}
		if (homeCategory.getPriority() != null) {
			existing.setPriority(homeCategory.getPriority());
		}
		
		return homecategoryRepository.save(existing);
	}

	@Override
	public List<HomeCategory> getAllHomeCategories() {
		return homecategoryRepository.findAllByOrderByPriorityAscIdAsc();
	}

	@Override
	public List<HomeCategory> getHomeCategoriesBySection(HomeCategorySection section) {
		if (section == null) {
			return getAllHomeCategories();
		}
		return homecategoryRepository.findBySectionOrderByPriorityAscIdAsc(section);
	}

	@Override
	public HomeCategory getHomeCategoryById(Long id) {
		return homecategoryRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Home category not found with ID: " + id));
	}

	@Override
	@Transactional
	public void deleteHomeCategory(Long id) {
		HomeCategory existing = homecategoryRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Home category not found with ID: " + id));

		// Unlink any Deal that references this home category to prevent FK constraint violations
		if (dealRepository != null) {
			Optional<Deal> dealOpt = dealRepository.findByCategory(existing);
			if (dealOpt.isPresent()) {
				Deal deal = dealOpt.get();
				deal.setCategory(null);
				dealRepository.save(deal);
			}
		}

		homecategoryRepository.delete(existing);
	}
}
