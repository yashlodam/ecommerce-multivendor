package com.zosh.response;

import java.util.List;

import com.zosh.model.Home;
import com.zosh.model.HomeCategory;

public interface HomeService {

	
	Home createHomePageData(List<HomeCategory> allCategories);
	
}

