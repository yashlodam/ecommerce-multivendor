package com.zosh.service;

import java.util.Set;

import com.razorpay.PaymentLink;
import com.razorpay.RazorpayException;
import com.zosh.model.Order;
import com.zosh.model.PaymentOrder;
import com.zosh.model.User;

public interface PaymentService {

	PaymentOrder createOrder(User user,Set<Order> orders);
	PaymentOrder getPaymentOrderById(Long orderId);
	PaymentOrder getPaymentOrderByPaymentId(String paymentId);
	Boolean ProceedPaymentOrder(PaymentOrder paymentOrder,String paymentId,String paymentLinkId) throws RazorpayException;
	
	PaymentLink createRazorpayPaymetnLink(User user,Long amount,Long orderId) throws RazorpayException;
	

	String createStripePaymentLink(User user,Long amount, Long orderId);
	
	
}
