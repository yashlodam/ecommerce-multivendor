package com.zosh.controller;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.razorpay.PaymentLink;
import com.razorpay.RazorpayException;
import com.zosh.domain.PaymentMethod;
import com.zosh.exceptions.SellerException;
import com.zosh.model.Address;
import com.zosh.model.Cart;
import com.zosh.model.Order;
import com.zosh.model.OrderItem;
import com.zosh.model.PaymentOrder;
import com.zosh.model.Seller;
import com.zosh.model.SellerReport;
import com.zosh.model.User;
import com.zosh.repository.PaymentOrderRepository;
import com.zosh.response.PaymentLinkResponse;
import com.zosh.service.CartService;
import com.zosh.service.OrderService;
import com.zosh.service.PaymentService;
import com.zosh.service.SellerReportService;
import com.zosh.service.SellerService;
import com.zosh.service.UserService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

	@Autowired
	private OrderService orderService;
	
	@Autowired
	private UserService userservice;
	
	@Autowired
	private CartService cartService;
	
	@Autowired
	private SellerService  sellerService;
	
	@Autowired
	private SellerReportService sellerReportService;
	
	@Autowired
	private PaymentService paymentService;
	
	@Autowired
	private PaymentOrderRepository paymentOrderRepository;
	
	
	
	
	@PostMapping
	public ResponseEntity<PaymentLinkResponse> createOrderHandler(
			@RequestBody Address shippingAddress,
			@RequestParam PaymentMethod paymentMethod,
			@RequestHeader("Authorization") String jwt
			) throws RazorpayException
	{
		
		User user = userservice.findUserByJwtToken(jwt);
		Cart cart = cartService.findUserCart(user);
		
		Set<Order> orders = orderService.createOrder(user, shippingAddress, cart);
		
		PaymentOrder paymentOrder = paymentService.createOrder(user, orders);
		
		PaymentLinkResponse res = new PaymentLinkResponse();
		
		if(paymentMethod.equals(PaymentMethod.RAZORPAY)) {
			PaymentLink payment = paymentService.createRazorpayPaymetnLink(user,
					paymentOrder.getAmount(),
					paymentOrder.getId()
					);
					
					String paymentUrl = payment.get("short_url");
					String paymentUrlId = payment.get("id");
					
					res.setPayment_link_url(paymentUrl);
					
					paymentOrder.setPaymentLinkId(paymentUrlId);
					paymentOrderRepository.save(paymentOrder);
		}
		else {
			String paymentUrl = paymentService.createStripePaymentLink(user,paymentOrder.getAmount(),paymentOrder.getId());
			
			res.setPayment_link_url(paymentUrl);
			
		}
		
		return new ResponseEntity<>(res,HttpStatus.OK);
	
    }
	
	
	@GetMapping("/user")
	public ResponseEntity<List<Order>> usersOrderHistoryHandler(
			@RequestHeader("Authorization") String jwt
			)
	{
		User user = userservice.findUserByJwtToken(jwt);
		List<Order> orders = orderService.usersOrderHistory(user.getId());
		
		return new ResponseEntity<>(orders,HttpStatus.ACCEPTED);
	}
	
	
	@GetMapping("/{orderId}")
	public ResponseEntity<Order> getOrderById(@PathVariable Long orderId,@RequestHeader("Authorization") String jwt){
		
		User user = userservice.findUserByJwtToken(jwt);
		Order orders = orderService.findOrderById(orderId);
		
		return new ResponseEntity<>(orders,HttpStatus.ACCEPTED);
	}
	
	
	@GetMapping("/item/{orderItemId}")
	public ResponseEntity<OrderItem> getOrderItemById(
			@PathVariable Long orderItemId, @RequestHeader("Authorization") String jwt
			)
	{
		User user = userservice.findUserByJwtToken(jwt);
		OrderItem orderitem = orderService.getOrderById(orderItemId);
		
		return new ResponseEntity<>(orderitem,HttpStatus.ACCEPTED);
		
	}
	
	
	@PutMapping("/{orderId}/cancel")
	public ResponseEntity<Order> cancelOrder(
			@PathVariable Long orderId,
			@RequestHeader("Authorization") String jwt
			) throws SellerException
	{
		
		User user = userservice.findUserByJwtToken(jwt);
		Order order = orderService.cancelOrder(orderId, user);
		
		Seller seller = sellerService.getSellerById(order.getSellerId());
		SellerReport report = sellerReportService.getSellerReport(seller);
		
     	report.setCanceledOrders(report.getCanceledOrders());
		report.setTotalRefunds(report.getTotalRefunds()+order.getTotalSellingPrice());
     	sellerReportService.updateSellerReport(report);
		
		return ResponseEntity.ok(order);
		
	}
	
	

	
	
	
}



