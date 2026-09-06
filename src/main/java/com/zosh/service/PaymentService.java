package com.zosh.service;



import com.razorpay.PaymentLink;
import com.razorpay.RazorpayException;
import com.zosh.domain.PaymentMethod;
import com.zosh.exceptions.SellerException;
import com.zosh.model.Address;
import com.zosh.model.Cart;
import com.zosh.model.PaymentOrder;
import com.zosh.model.User;

public interface PaymentService {

	PaymentOrder createOrder(User user,
	        Cart cart,
	        Address shippingAddress,
	        PaymentMethod paymentMethod);
	PaymentOrder getPaymentOrderById(Long orderId);
	PaymentOrder getPaymentOrderByPaymentId(String paymentId);
	Boolean ProceedPaymentOrder(PaymentOrder paymentOrder,String paymentId,String paymentLinkId) throws RazorpayException, SellerException;
	
	PaymentLink createRazorpayPaymetnLink(User user,Long amount,Long orderId) throws RazorpayException;
	com.razorpay.Order createRazorpayOrder(User user, Long amount, Long orderId) throws RazorpayException;
	String getRazorpayKeyId();
}
