package com.zosh.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zosh.domain.HomeCategorySection;
import com.zosh.model.Deal;
import com.zosh.model.Home;
import com.zosh.model.HomeCategory;
import com.zosh.repository.DealRepository;
import com.zosh.response.HomeService;

@Service
public class HomeServiceImpl implements HomeService {

	private static final Logger log = LoggerFactory.getLogger(HomeServiceImpl.class);
	
	@Autowired
	private DealRepository dealRepository;
	

	@Override
	public Home createHomePageData(List<HomeCategory> allCategories) {

	    List<HomeCategory> gridCategories = allCategories.stream()
	            .filter(category -> category.getSection() == HomeCategorySection.GRID)
	            .collect(Collectors.toList());

	    List<HomeCategory> shopByCategories = allCategories.stream()
	            .filter(category -> category.getSection() == HomeCategorySection.SHOP_BY_CATEGORIES)
	            .collect(Collectors.toList());

	    List<HomeCategory> electricCategories = allCategories.stream()
	            .filter(category -> category.getSection() == HomeCategorySection.ELECTRIC_CATEGORIES)
	            .collect(Collectors.toList());

	    List<HomeCategory> dealCategories = allCategories.stream()
	            .filter(category -> category.getSection() == HomeCategorySection.DEALS)
	            .collect(Collectors.toList());
	    List<Deal> createdDeals = new ArrayList<>();
	    
	    if(dealRepository.findAll().isEmpty()) {
	    	List<Deal> deals = allCategories.stream()
	    			.filter(category-> category.getSection() == HomeCategorySection.DEALS)
	    			.map(category-> new Deal(null,10,category))
	    			.collect(Collectors.toList());
	    	
	    	createdDeals = dealRepository.saveAll(deals);
	    	for (Deal deal : createdDeals) {
	    	    log.debug("Created Deal ID = {}, Category = {}", deal.getId(),
	    	            deal.getCategory() != null ? deal.getCategory().getCategoryId() : "NULL");
	    	}
	    	
	    } else createdDeals = dealRepository.findAll();
	    
	    
	    

	    Home home = new Home();

	    home.setGrid(gridCategories);
	    home.setShopByCategories(shopByCategories);
	    home.setElectricCategories(electricCategories);
	    home.setDeals(createdDeals);
	    home.setDealCategories(dealCategories);

	    return home;
	}

}
