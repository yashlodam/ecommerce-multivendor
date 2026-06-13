package com.zosh.model;

import java.util.List;

public class Home {

	
	private List<HomeCategory> grid;
	
	private List<HomeCategory> shopByCategories;
	
	private List<HomeCategory> electricCategories;
	
	private List<HomeCategory> dealCategories;
	
	private List<Deal> deals;

	public List<HomeCategory> getGrid() {
		return grid;
	}

	public void setGrid(List<HomeCategory> grid) {
		this.grid = grid;
	}

	public List<HomeCategory> getShopByCategories() {
		return shopByCategories;
	}

	public void setShopByCategories(List<HomeCategory> shopByCategories) {
		this.shopByCategories = shopByCategories;
	}

	public List<HomeCategory> getElectricCategories() {
		return electricCategories;
	}

	public void setElectricCategories(List<HomeCategory> electricCategories) {
		this.electricCategories = electricCategories;
	}

	public List<HomeCategory> getDealCategories() {
		return dealCategories;
	}

	public void setDealCategories(List<HomeCategory> dealCategories) {
		this.dealCategories = dealCategories;
	}

	public List<Deal> getDeals() {
		return deals;
	}

	public void setDeals(List<Deal> deals) {
		this.deals = deals;
	}
	
}
