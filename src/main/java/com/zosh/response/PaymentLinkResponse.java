package com.zosh.response;

public class PaymentLinkResponse {

	private String payment_link_url;
	private String payment_link_id;
	private String razorpay_order_id;
	private Long amount;
	private String currency;
	private String key_id;
	private Long payment_order_id;

	public String getPayment_link_url() {
		return payment_link_url;
	}
	public void setPayment_link_url(String payment_link_url) {
		this.payment_link_url = payment_link_url;
	}
	public String getPayment_link_id() {
		return payment_link_id;
	}
	public void setPayment_link_id(String payment_link_id) {
		this.payment_link_id = payment_link_id;
	}
	public String getRazorpay_order_id() {
		return razorpay_order_id;
	}
	public void setRazorpay_order_id(String razorpay_order_id) {
		this.razorpay_order_id = razorpay_order_id;
	}
	public Long getAmount() {
		return amount;
	}
	public void setAmount(Long amount) {
		this.amount = amount;
	}
	public String getCurrency() {
		return currency;
	}
	public void setCurrency(String currency) {
		this.currency = currency;
	}
	public String getKey_id() {
		return key_id;
	}
	public void setKey_id(String key_id) {
		this.key_id = key_id;
	}
	public Long getPayment_order_id() {
		return payment_order_id;
	}
	public void setPayment_order_id(Long payment_order_id) {
		this.payment_order_id = payment_order_id;
	}
}
