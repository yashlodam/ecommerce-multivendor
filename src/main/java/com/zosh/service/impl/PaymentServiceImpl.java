package com.zosh.service.impl;

import java.util.Set;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.razorpay.Payment;
import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.zosh.domain.PaymentOrderStatus;
import com.zosh.domain.PaymentStatus;
import com.zosh.model.Order;
import com.zosh.model.PaymentOrder;
import com.zosh.model.User;
import com.zosh.repository.OrderRepository;
import com.zosh.repository.PaymentOrderRepository;
import com.zosh.service.PaymentService;

@Service
public class PaymentServiceImpl implements PaymentService {

	@Autowired
	private PaymentOrderRepository paymentOrderRepository;
	
	@Autowired
	private OrderRepository  orderRepository;
	
	private String apikey = "rzp_test_T2Zd0oo1kXFV4t";
	private String apiSecret = "LdMgkLnQLjJR5M8iqzEL5riy";
	
	@Override
	public PaymentOrder createOrder(User user, Set<Order> orders) {
		
		Long amount = orders.stream().mapToLong(Order::getTotalSellingPrice).sum();
		
		PaymentOrder paymentOrder =  new PaymentOrder();
		paymentOrder.setAmount(amount);
		paymentOrder.setUser(user);
		paymentOrder.setOrders(orders);
		
		
		return paymentOrderRepository.save(paymentOrder);
	}

	@Override
	public PaymentOrder getPaymentOrderById(Long orderId) {
		
		return paymentOrderRepository.findById(orderId).orElseThrow(()-> new IllegalArgumentException("Payment order not found"));
	}

	@Override
	public PaymentOrder getPaymentOrderByPaymentId(String paymentId) {
		PaymentOrder order = paymentOrderRepository.findByPaymentLinkId(paymentId);
		if(order==null) {
			throw new IllegalArgumentException("payment order not found with provided  paymennt link id");
		}
		return order;
	}

	@Override
	public Boolean ProceedPaymentOrder(PaymentOrder paymentOrder, String paymentId, String paymentLinkId) throws RazorpayException {
		
		if(paymentOrder.getStatus().equals(PaymentOrderStatus.PENDING)) {
			RazorpayClient razorpay = new RazorpayClient(apikey,apiSecret);
			
			Payment payment = razorpay.payments.fetch(paymentId);
			
			String status = payment.get("status");
			if(status.equals("captured")) {
				Set<Order> orders = paymentOrder.getOrders();
				for(Order order:orders) {
					order.setPaymenntStatus(PaymentStatus.COMPLETED);
					orderRepository.save(order);
				}
				paymentOrder.setStatus(PaymentOrderStatus.SUCESS);
				paymentOrderRepository.save(paymentOrder);
				return true;
			}
			
			paymentOrder.setStatus(PaymentOrderStatus.FAILED);
			paymentOrderRepository.save(paymentOrder);
			return false;
		}
		
		return false;
	}

	@Override
	public PaymentLink createRazorpayPaymetnLink(User user, Long amount, Long orderId) throws RazorpayException {
		
		amount = amount * 100;
		
		try {
			
			RazorpayClient razorpay = new RazorpayClient(apikey,apiSecret);
			
			JSONObject payentLinkRequest = new JSONObject();
			
			payentLinkRequest.put("amount", amount);
			payentLinkRequest.put("currency", "INR");
			
			JSONObject customer = new JSONObject();
			customer.put("name", user.getFullName());
			customer.put("email", user.getEmail());
			payentLinkRequest.put("customer", customer);
			
			
			JSONObject notify = new JSONObject();
			notify.put("email", true);
			payentLinkRequest.put("notify", notify);
			
			payentLinkRequest.put("callback_url","http://localhost:5173/payment-success/"+orderId);
			
			payentLinkRequest.put("callback_method", "get");
			
			PaymentLink paymentLink = razorpay.paymentLink.create(payentLinkRequest);
			
			String paymentLinkUrl = paymentLink.get("short_url");
			String payementLinkId = paymentLink.get("id");
			
			
			return paymentLink;
			
		}
		catch(Exception e) {
			
			throw new RazorpayException(e.getMessage());
		}
		
		
	}

	@Override
	public String createStripePaymentLink(User user, Long amount, Long orderId) {
		// TODO Auto-generated method stub
		return null;
	}

	
	
}
