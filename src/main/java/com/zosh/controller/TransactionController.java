package com.zosh.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zosh.model.Seller;
import com.zosh.model.Transaction;
import com.zosh.service.SellerService;
import com.zosh.service.TransactionService;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

	@Autowired
	private TransactionService transactionService;
	
	@Autowired
	private SellerService sellerService;
	
	@GetMapping("/seller")
	public ResponseEntity<List<Transaction>> getTransactionBySeller(
			@RequestHeader("Authorization") String jwt
			)
	
	{
		
		Seller seller = sellerService.getSellerProfile(jwt);
		
		List<Transaction> transactions = transactionService.getTransactionBySellerId(seller);
		
		return ResponseEntity.ok(transactions);
		
		
	}
	
	
	@GetMapping
	public ResponseEntity<List<Transaction>> getAllTransactions(){
		
		List<Transaction> transactions = transactionService.getAllTransactions();
		
		return ResponseEntity.ok(transactions);
	}
	
	
	
	
	
	
	
	
	
}
