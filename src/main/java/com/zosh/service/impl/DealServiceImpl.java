package com.zosh.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zosh.model.Deal;
import com.zosh.model.HomeCategory;
import com.zosh.repository.DealRepository;
import com.zosh.repository.HomeCategoryRepository;
import com.zosh.service.DealService;

@Service
public class DealServiceImpl implements DealService {
	
	@Autowired
	private DealRepository dealRepository;
	
	@Autowired
	private HomeCategoryRepository repository;

	@Override
	public List<Deal> getDeals() {
		// TODO Auto-generated method stub
		return dealRepository.findAll();
	}

	@Override
	public Deal createDeal(Deal deal) {
		
		HomeCategory category = repository.findById(deal.getCategory().getId()).orElseThrow(()-> new RuntimeException("not found"));
		
		
		
		Deal newDeal = dealRepository.save(deal);
		newDeal.setCategory(category);
		newDeal.setDiscount(deal.getDiscount());
		
		
		return dealRepository.save(newDeal);
	}

	@Override
	public Deal updateDeal(Deal deal,Long id) {
		
		Deal existingDeal = dealRepository.findById(id).orElseThrow(()-> new RuntimeException("Deal not found with id"));
		
		HomeCategory category = repository.findById(deal.getCategory().getId()).orElseThrow(()-> new RuntimeException("Not found"));
		
		if(existingDeal!=null) {
			if(deal.getDiscount()!=null) {
				existingDeal.setDiscount(deal.getDiscount());
			}
			if(category!=null) {
				existingDeal.setCategory(category);
			}
			return dealRepository.save(existingDeal);
		}
		
		throw new RuntimeException("Deal not found");
	}

	@Override
	public void deleteDeal(Long id) {
		
		Deal deals = dealRepository.findById(id).orElseThrow(()-> new RuntimeException("deal not found with id"));
		
		dealRepository.delete(deals);
		
		
	}

}
