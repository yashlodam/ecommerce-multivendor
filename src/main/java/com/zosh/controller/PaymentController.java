package com.zosh.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.razorpay.RazorpayException;
import com.zosh.exceptions.SellerException;
import com.zosh.model.Order;
import com.zosh.model.PaymentOrder;
import com.zosh.model.Seller;
import com.zosh.model.SellerReport;
import com.zosh.model.User;
import com.zosh.response.ApiResponse;
import com.zosh.response.PaymentLinkResponse;
import com.zosh.service.PaymentService;
import com.zosh.service.SellerReportService;
import com.zosh.service.SellerService;
import com.zosh.service.TransactionService;
import com.zosh.service.UserService;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

	@Autowired
	private PaymentService paymentService;
	
	@Autowired
	private UserService service;
	
	@Autowired
	private SellerService sellerService;
	
	@Autowired
	private SellerReportService reportService;
	
	@Autowired
	private TransactionService transactionService;
	
	
	@GetMapping("/{paymentId}")
	public ResponseEntity<ApiResponse> paymentSucessHandler(
			
			@PathVariable String paymentId,
			@RequestParam String paymentLinkId,
			@RequestHeader("Authorization") String jwt
			
			) throws RazorpayException, SellerException
	{
		
		User user = service.findUserByJwtToken(jwt);
		
		PaymentLinkResponse paymentLinkResponse;
		
		PaymentOrder paymentOrder = paymentService.getPaymentOrderByPaymentId(paymentId);
		
		boolean paymentSucess = paymentService.ProceedPaymentOrder(paymentOrder, paymentId, paymentLinkId);
		
		
		if(paymentSucess) {
			
			
			for(Order order:paymentOrder.getOrders()) {
				
				transactionService.createTransaction(order);
				
				Seller seller = sellerService.getSellerById(order.getSellerId());
				
				SellerReport report = reportService.getSellerReport(seller);
				report.setTotalOrders(report.getTotalOrders()+1);
				report.setTotalEarnings(report.getTotalEarnings()+order.getTotalSellingPrice());
				report.setTotalSales(report.getTotalSales()+order.getOrderItems().size());
				reportService.updateSellerReport(report);
				
				
				
			}
			
			
		}
		
		ApiResponse res = new ApiResponse();
		res.setMessage("Payment sucessfull");
		
		return new ResponseEntity<>(res,HttpStatus.CREATED);
		
		
	}
	
}
