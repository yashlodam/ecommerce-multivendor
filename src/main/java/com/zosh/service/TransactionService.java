package com.zosh.service;

import java.util.List;

import com.zosh.model.Order;
import com.zosh.model.Seller;
import com.zosh.model.Transaction;

public interface TransactionService {

	Transaction createTransaction(Order order);
	
	List<Transaction> getTransactionBySellerId(Seller seller);
	
	List<Transaction> getAllTransactions();
}
