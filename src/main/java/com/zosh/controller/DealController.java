package com.zosh.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zosh.model.Deal;
import com.zosh.response.ApiResponse;
import com.zosh.service.DealService;

@RestController
@RequestMapping("/admin/deals")
public class DealController {

	@Autowired
	private DealService dealService;
	
	
	@PostMapping
	public ResponseEntity<Deal> createDeals(
			@RequestBody Deal deals
			)
	{
		Deal createdDeals = dealService.createDeal(deals);
		
		return new ResponseEntity<>(createdDeals,HttpStatus.ACCEPTED);
	}
	
	@PatchMapping("/{id}")
	public ResponseEntity<Deal> updateDeal(
			@PathVariable Long id,
			@RequestBody Deal deal
			)
	{
		Deal updateDeal = dealService.updateDeal(deal, id);
		return ResponseEntity.ok(updateDeal);
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse> deleteDeals(
			@PathVariable Long id
			)
	{
		dealService.deleteDeal(id);
		ApiResponse res = new ApiResponse();
		res.setMessage("Deal deleted");
		return new ResponseEntity<>(res,HttpStatus.ACCEPTED);
	}
	
	
	
	
	
	
	
	
	
	
}
