package com.zosh.model;

import com.zosh.domain.PaymentStatus;

public class PaymentDetails {

	private String paymentId;
	
	private String razorpayPaymentLinkId;
	private String razorpayPaymentLinkReferenceId;
	private String razorpayPaymentLinkStatus;
	private String razorpayPaymentIdZWSP;
	private PaymentStatus status;
	public String getPaymentId() {
		return paymentId;
	}
	public void setPaymentId(String paymentId) {
		this.paymentId = paymentId;
	}
	public String getRazorpayPaymentLinkId() {
		return razorpayPaymentLinkId;
	}
	public void setRazorpayPaymentLinkId(String razorpayPaymentLinkId) {
		this.razorpayPaymentLinkId = razorpayPaymentLinkId;
	}
	public String getRazorpayPaymentLinkReferenceId() {
		return razorpayPaymentLinkReferenceId;
	}
	public void setRazorpayPaymentLinkReferenceId(String razorpayPaymentLinkReferenceId) {
		this.razorpayPaymentLinkReferenceId = razorpayPaymentLinkReferenceId;
	}
	public String getRazorpayPaymentLinkStatus() {
		return razorpayPaymentLinkStatus;
	}
	public void setRazorpayPaymentLinkStatus(String razorpayPaymentLinkStatus) {
		this.razorpayPaymentLinkStatus = razorpayPaymentLinkStatus;
	}
	public String getRazorpayPaymentIdZWSP() {
		return razorpayPaymentIdZWSP;
	}
	public void setRazorpayPaymentIdZWSP(String razorpayPaymentIdZWSP) {
		this.razorpayPaymentIdZWSP = razorpayPaymentIdZWSP;
	}
	public PaymentStatus getStatus() {
		return status;
	}
	public void setStatus(PaymentStatus status) {
		this.status = status;
	}
	
}
