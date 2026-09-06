package com.zosh.controller;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.razorpay.PaymentLink;
import com.razorpay.RazorpayException;
import com.zosh.domain.OrderStatus;
import com.zosh.domain.PaymentMethod;
import com.zosh.domain.PaymentStatus;
import com.zosh.domain.USER_ROLE;
import com.zosh.exceptions.SellerException;
import com.zosh.model.Address;
import com.zosh.model.Cart;
import com.zosh.model.Order;
import com.zosh.model.OrderItem;
import com.zosh.model.PaymentOrder;
import com.zosh.model.Seller;
import com.zosh.model.SellerReport;
import com.zosh.model.User;
import com.zosh.repository.OrderRepository;
import com.zosh.repository.PaymentOrderRepository;
import com.zosh.response.PaymentLinkResponse;
import com.zosh.service.CartService;
import com.zosh.service.OrderService;
import com.zosh.service.PaymentService;
import com.zosh.service.SellerReportService;
import com.zosh.service.SellerService;
import com.zosh.service.TransactionService;
import com.zosh.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Customer - Orders", description = "Order creation, history, and status management endpoints")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    @Autowired private OrderService orderService;
    @Autowired private UserService userservice;
    @Autowired private CartService cartService;
    @Autowired private SellerService sellerService;
    @Autowired private SellerReportService sellerReportService;
    @Autowired private PaymentService paymentService;
    @Autowired private PaymentOrderRepository paymentOrderRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private TransactionService transactionService;

    @PostMapping
    @Operation(summary = "Create order and initialize payment link (COD / Razorpay)")
    public ResponseEntity<PaymentLinkResponse> createOrderHandler(
            @Valid @RequestBody Address shippingAddress,
            @RequestParam PaymentMethod paymentMethod,
            @RequestHeader("Authorization") String jwt
    ) throws RazorpayException, SellerException {

        User user = userservice.findUserByJwtToken(jwt);
        Cart cart = cartService.findUserCart(user);

        PaymentLinkResponse res = new PaymentLinkResponse();

        // ================= COD =================
        if (paymentMethod == PaymentMethod.COD) {
            Set<Order> orders = orderService.createOrder(user, shippingAddress, cart);

            for (Order order : orders) {
                // For Cash on Delivery, order is formally placed
                order.setOrderStatus(OrderStatus.PLACED);
                order.setPaymentStatus(PaymentStatus.PENDING);
                orderRepository.save(order);

                transactionService.createTransaction(order);

                try {
                    if (order.getSellerId() != null) {
                        Seller seller = sellerService.getSellerById(order.getSellerId());
                        if (seller != null) {
                            SellerReport report = sellerReportService.getSellerReport(seller);
                            if (report != null) {
                                report.setTotalOrders(report.getTotalOrders() + 1);
                                int itemsCount = order.getOrderItems() != null ? order.getOrderItems().size() : 0;
                                report.setTotalSales(report.getTotalSales() + itemsCount);
                                long earning = order.getTotalSellingPrice() != null ? order.getTotalSellingPrice() : 0L;
                                report.setTotalEarnings(report.getTotalEarnings() + earning);
                                sellerReportService.updateSellerReport(report);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Non-fatal: Failed to update seller report for COD order {}: {}", order.getId(), e.getMessage());
                }
            }

            cartService.clearCart(user);

            res.setPayment_link_id(null);
            res.setPayment_link_url(null);

            return ResponseEntity.ok(res);
        }

        // ================= ONLINE PAYMENT =================
        PaymentOrder paymentOrder = paymentService.createOrder(user, cart, shippingAddress, paymentMethod);

        if (paymentMethod == PaymentMethod.RAZORPAY) {
            com.razorpay.Order razorpayOrder = paymentService.createRazorpayOrder(
                    user, paymentOrder.getAmount(), paymentOrder.getId());

            String razorpayOrderId = razorpayOrder.get("id");
            long amountInPaise = paymentOrder.getAmount() * 100;

            res.setRazorpay_order_id(razorpayOrderId);
            res.setPayment_link_id(razorpayOrderId);
            res.setAmount(amountInPaise);
            res.setCurrency("INR");
            res.setKey_id(paymentService.getRazorpayKeyId());
            res.setPayment_order_id(paymentOrder.getId());

            // Also try to provide hosted fallback URL if available
            try {
                PaymentLink payment = paymentService.createRazorpayPaymetnLink(
                        user, paymentOrder.getAmount(), paymentOrder.getId());
                res.setPayment_link_url(payment.get("short_url"));
            } catch (Exception e) {
                log.info("Hosted payment link creation skipped or fallback not needed: {}", e.getMessage());
            }
        }

        return ResponseEntity.ok(res);
    }

    @GetMapping("/user")
    @Operation(summary = "Get order history for authenticated customer")
    public ResponseEntity<List<Order>> usersOrderHistoryHandler(
            @RequestHeader("Authorization") String jwt) {

        User user = userservice.findUserByJwtToken(jwt);
        List<Order> orders = orderService.usersOrderHistory(user.getId());

        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order details (enforces customer/admin authorization check)")
    public ResponseEntity<Order> getOrderById(
            @PathVariable Long orderId,
            @RequestHeader("Authorization") String jwt) {

        User user = userservice.findUserByJwtToken(jwt);
        Order order = orderService.findOrderById(orderId);

        // IDOR Protection: User can only access their own order, unless admin
        if (!order.getUser().getId().equals(user.getId()) && user.getRole() != USER_ROLE.ROLE_ADMIN) {
            throw new AccessDeniedException("You do not have permission to view this order.");
        }

        return ResponseEntity.ok(order);
    }

    @GetMapping("/item/{orderItemId}")
    @Operation(summary = "Get order item details (enforces customer/admin authorization check)")
    public ResponseEntity<OrderItem> getOrderItemById(
            @PathVariable Long orderItemId,
            @RequestHeader("Authorization") String jwt) {

        User user = userservice.findUserByJwtToken(jwt);
        OrderItem orderItem = orderService.getOrderById(orderItemId);

        // IDOR Protection
        if (!orderItem.getUserId().equals(user.getId()) && user.getRole() != USER_ROLE.ROLE_ADMIN) {
            throw new AccessDeniedException("You do not have permission to view this order item.");
        }

        return ResponseEntity.ok(orderItem);
    }

    @PutMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel an order and restore reserved product inventory")
    public ResponseEntity<Order> cancelOrder(
            @PathVariable Long orderId,
            @RequestHeader("Authorization") String jwt) {

        User user = userservice.findUserByJwtToken(jwt);
        Order order = orderService.cancelOrder(orderId, user);

        // Update seller report on cancellation safely
        try {
            if (order.getSellerId() != null) {
                Seller seller = sellerService.getSellerById(order.getSellerId());
                if (seller != null) {
                    SellerReport report = sellerReportService.getSellerReport(seller);
                    if (report != null) {
                        report.setCanceledOrders(report.getCanceledOrders() + 1);
                        long price = order.getTotalSellingPrice() != null ? order.getTotalSellingPrice() : 0L;
                        report.setTotalRefunds(report.getTotalRefunds() + price);
                        sellerReportService.updateSellerReport(report);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Non-fatal: Failed to update seller report for cancelled order {}: {}", orderId, e.getMessage());
        }

        return ResponseEntity.ok(order);
    }
}
