package com.zosh.service;

import java.util.List;

import com.zosh.model.Deal;

public interface DealService {

	List<Deal> getDeals();
	
	Deal createDeal(Deal deal);
	Deal updateDeal(Deal deal,Long id);
	void deleteDeal(Long id);
	
}
