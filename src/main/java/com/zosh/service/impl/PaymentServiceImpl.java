package com.zosh.service.impl;

import java.util.Set;


import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.razorpay.Payment;
import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.zosh.domain.OrderStatus;
import com.zosh.domain.PaymentOrderStatus;
import com.zosh.domain.PaymentStatus;
import com.zosh.exceptions.ResourceNotFoundException;
import com.zosh.exceptions.SellerException;
import com.zosh.model.Cart;
import com.zosh.model.Order;
import com.zosh.model.PaymentOrder;
import com.zosh.model.Seller;
import com.zosh.model.SellerReport;
import com.zosh.model.User;
import com.zosh.domain.NotificationType;
import com.zosh.repository.AddressRepository;
import com.zosh.repository.OrderRepository;
import com.zosh.repository.PaymentOrderRepository;
import com.zosh.repository.UserRepository;
import com.zosh.service.CartService;
import com.zosh.service.NotificationService;
import com.zosh.service.OrderService;
import com.zosh.service.PaymentService;
import com.zosh.service.SellerReportService;
import com.zosh.service.SellerService;
import com.zosh.service.TransactionService;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    @Autowired private PaymentOrderRepository paymentOrderRepository;
    @Autowired private CartService cartService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private SellerService sellerService;
    @Autowired private SellerReportService reportService;
    @Autowired private TransactionService transactionService;
    @Autowired private OrderService orderService;
    @Autowired private AddressRepository addressRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationService notificationService;

    // ---- Configuration from application.properties / environment variables ----

    /** Loaded from RAZORPAY_KEY_ID environment variable */
    @Value("${payment.razorpay.key-id:}")
    private String razorpayKeyId;

    /** Loaded from RAZORPAY_KEY_SECRET environment variable */
    @Value("${payment.razorpay.key-secret:}")
    private String razorpayKeySecret;

    /** Frontend base URL for payment redirect — configurable per environment */
    @Value("${app.payment.callback-base-url:http://localhost:5173}")
    private String callbackBaseUrl;

    @Override
    @Transactional
    public PaymentOrder createOrder(
            User user,
            Cart cart,
            com.zosh.model.Address shippingAddress,
            com.zosh.domain.PaymentMethod paymentMethod) {

        // Resolve or save shipping address to prevent transient entity issues
        com.zosh.model.Address address = shippingAddress;
        if (shippingAddress != null) {
            if (shippingAddress.getId() != null) {
                address = addressRepository.findById(shippingAddress.getId())
                        .orElse(shippingAddress);
            } else {
                address = addressRepository.save(shippingAddress);
                if (user != null && user.getAddresses() != null) {
                    user.getAddresses().add(address);
                    userRepository.save(user);
                }
            }
        }

        long amount = Math.round(cart.getTotalSellingPrice());
        if (amount <= 0) {
            amount = cart.getCartItems().stream()
                    .mapToLong(item -> (long) item.getSellingPrice())
                    .sum();
        }

        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setAmount(amount);
        paymentOrder.setUser(user);
        paymentOrder.setShippingAddress(address);
        paymentOrder.setPaymentMethod(paymentMethod);
        paymentOrder.setStatus(PaymentOrderStatus.PENDING);

        return paymentOrderRepository.save(paymentOrder);
    }

    @Override
    public PaymentOrder getPaymentOrderById(Long orderId) {
        return paymentOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentOrder", "id", orderId));
    }

    @Override
    public PaymentOrder getPaymentOrderByPaymentId(String paymentId) {
        if (paymentId == null || paymentId.isBlank()) {
            throw new ResourceNotFoundException("PaymentOrder", "paymentLinkId", "null");
        }

        PaymentOrder order = null;

        // 1. If identifier starts with "order_", check razorpayOrderId first, then paymentLinkId
        if (paymentId.startsWith("order_")) {
            order = paymentOrderRepository.findByRazorpayOrderId(paymentId);
            if (order == null) {
                order = paymentOrderRepository.findByPaymentLinkId(paymentId);
            }
        }
        // 2. If identifier starts with "plink_", check paymentLinkId first, then razorpayOrderId
        else if (paymentId.startsWith("plink_")) {
            order = paymentOrderRepository.findByPaymentLinkId(paymentId);
            if (order == null) {
                order = paymentOrderRepository.findByRazorpayOrderId(paymentId);
            }
        }
        // 3. Fallback: check paymentLinkId first, then razorpayOrderId
        else {
            order = paymentOrderRepository.findByPaymentLinkId(paymentId);
            if (order == null) {
                order = paymentOrderRepository.findByRazorpayOrderId(paymentId);
            }
        }

        if (order != null) {
            return order;
        }

        // 4. Try numeric database ID lookup (e.g. if orderRef or paymentOrderId was passed)
        try {
            Long numericId = Long.parseLong(paymentId);
            return paymentOrderRepository.findById(numericId)
                    .orElseThrow(() -> new ResourceNotFoundException("PaymentOrder", "id", paymentId));
        } catch (NumberFormatException ignored) {
            // Not a numeric ID
        }

        throw new ResourceNotFoundException("PaymentOrder", "paymentLinkId", paymentId);
    }

    @Override
    public PaymentOrder resolvePaymentOrderFromRazorpayPayment(String paymentId) {
        if (paymentId == null || paymentId.isBlank()) {
            return null;
        }
        if (razorpayKeyId == null || razorpayKeyId.isBlank() || razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
            return null;
        }

        try {
            RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            Payment payment = razorpay.payments.fetch(paymentId);

            if (payment != null) {
                // 1. Try finding by Razorpay order_id
                if (payment.has("order_id")) {
                    Object orderIdObj = payment.get("order_id");
                    if (orderIdObj != null) {
                        String rzpOrderId = orderIdObj.toString();
                        if (!rzpOrderId.isBlank() && !"null".equalsIgnoreCase(rzpOrderId)) {
                            PaymentOrder order = paymentOrderRepository.findByRazorpayOrderId(rzpOrderId);
                            if (order != null) {
                                log.info("Resolved PaymentOrder #{} via Razorpay payment order_id: {}", order.getId(), rzpOrderId);
                                return order;
                            }
                            order = paymentOrderRepository.findByPaymentLinkId(rzpOrderId);
                            if (order != null) {
                                log.info("Resolved PaymentOrder #{} via paymentLinkId fallback: {}", order.getId(), rzpOrderId);
                                return order;
                            }
                        }
                    }
                }

                // 2. Try finding by notes.paymentOrderId
                if (payment.has("notes")) {
                    JSONObject notes = payment.get("notes");
                    if (notes != null && notes.has("paymentOrderId")) {
                        Object poIdObj = notes.get("paymentOrderId");
                        if (poIdObj != null) {
                            try {
                                Long poId = Long.parseLong(poIdObj.toString());
                                PaymentOrder order = paymentOrderRepository.findById(poId).orElse(null);
                                if (order != null) {
                                    log.info("Resolved PaymentOrder #{} via Razorpay payment note 'paymentOrderId'", order.getId());
                                    return order;
                                }
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not resolve payment order from Razorpay payment {}: {}", paymentId, e.getMessage());
        }

        return null;
    }

    /**
     * Verifies payment with Razorpay and — on success — creates orders, updates
     * seller reports, records transactions, and clears the cart.
     *
     * This entire method is @Transactional: if anything fails after Razorpay
     * confirms payment, all DB changes roll back and can be retried.
     *
     * IMPORTANT: In production, use Razorpay webhook instead of client-side callback
     * for stronger security (signature verification).
     */
    @Override
    @Transactional
    public Boolean ProceedPaymentOrder(
            PaymentOrder paymentOrder,
            String paymentId,
            String paymentLinkId) throws RazorpayException, SellerException {

        log.info("Processing payment: paymentOrderId={} status={}",
                paymentOrder.getId(), paymentOrder.getStatus());

        if (paymentOrder.getStatus() != PaymentOrderStatus.PENDING) {
            log.warn("PaymentOrder {} already processed — status={}",
                    paymentOrder.getId(), paymentOrder.getStatus());
            return paymentOrder.getStatus() == PaymentOrderStatus.SUCESS;
        }

        if (razorpayKeyId == null || razorpayKeyId.isBlank() || razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
            throw new RazorpayException("Razorpay API credentials are not configured. Please set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET.");
        }

        RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

        try {
            Payment payment = razorpay.payments.fetch(paymentId);
            String status = payment.get("status");

            log.info("Razorpay payment {} status: {}", paymentId, status);

            if ("authorized".equals(status)) {
                log.info("Payment {} is authorized; attempting auto-capture...", paymentId);
                try {
                    JSONObject captureRequest = new JSONObject();
                    Object amt = payment.get("amount");
                    Object cur = payment.get("currency");
                    captureRequest.put("amount", amt != null ? Long.parseLong(amt.toString()) : paymentOrder.getAmount() * 100);
                    captureRequest.put("currency", cur != null ? cur.toString() : "INR");
                    payment = razorpay.payments.capture(paymentId, captureRequest);
                    status = payment.get("status");
                    log.info("Payment {} captured status: {}", paymentId, status);
                } catch (Exception capEx) {
                    log.error("Failed to auto-capture authorized payment {}: {}", paymentId, capEx.getMessage());
                }
            }

            if ("captured".equals(status)) {
                Cart cart = cartService.findUserCart(paymentOrder.getUser());

                // Create sub-orders per seller with inventory deduction
                Set<Order> orders = orderService.createOrder(
                        paymentOrder.getUser(),
                        paymentOrder.getShippingAddress(),
                        cart);

                for (Order order : orders) {
                    order.setOrderStatus(OrderStatus.PLACED);
                    order.setPaymentStatus(PaymentStatus.COMPLETED);
                    orderRepository.save(order);

                    // Record transaction
                    transactionService.createTransaction(order);

                    // Update seller analytics
                    try {
                        Seller seller = sellerService.getSellerById(order.getSellerId());
                        SellerReport report = reportService.getSellerReport(seller);
                        report.setTotalOrders(report.getTotalOrders() + 1);
                        report.setTotalSales(report.getTotalSales() + order.getOrderItems().size());
                        report.setTotalEarnings(report.getTotalEarnings() + order.getTotalSellingPrice());
                        reportService.updateSellerReport(report);
                    } catch (SellerException e) {
                        log.error("Failed to update seller report for order {}: {}",
                                order.getId(), e.getMessage());
                        // Don't fail the whole transaction — report can be reconciled later
                    }
                }

                paymentOrder.setStatus(PaymentOrderStatus.SUCESS);
                paymentOrderRepository.save(paymentOrder);

                cartService.clearCart(paymentOrder.getUser());

                log.info("Payment processed successfully: paymentOrderId={}", paymentOrder.getId());

                // Notify Customer of successful payment
                notificationService.notifyUser(
                    paymentOrder.getUser(),
                    NotificationType.PAYMENT_SUCCESS,
                    "Payment Successful",
                    "Payment of ₹" + paymentOrder.getAmount() + " was completed successfully.",
                    String.valueOf(paymentOrder.getId()),
                    "PAYMENT",
                    "/account/orders"
                );

                return true;
            }

        } catch (RazorpayException e) {
            log.error("Razorpay verification failed for payment {}: {}", paymentId, e.getMessage());
            throw e;
        }

        paymentOrder.setStatus(PaymentOrderStatus.FAILED);
        paymentOrderRepository.save(paymentOrder);

        log.warn("Payment failed: paymentOrderId={} paymentId={}", paymentOrder.getId(), paymentId);

        // Notify Customer of failed payment
        notificationService.notifyUser(
            paymentOrder.getUser(),
            NotificationType.PAYMENT_FAILED,
            "Payment Failed",
            "Payment of ₹" + paymentOrder.getAmount() + " could not be processed. Please try again.",
            String.valueOf(paymentOrder.getId()),
            "PAYMENT",
            "/cart"
        );

        // Alert Admins
        notificationService.broadcastToAdmins(
            NotificationType.PAYMENT_ISSUE,
            "Payment Failed Alert",
            "Payment of ₹" + paymentOrder.getAmount() + " failed for customer " + (paymentOrder.getUser() != null ? paymentOrder.getUser().getEmail() : "Unknown") + " (PaymentOrder #" + paymentOrder.getId() + ").",
            String.valueOf(paymentOrder.getId()),
            "PAYMENT",
            "/admin/transactions"
        );

        return false;
    }

    @Override
    public PaymentLink createRazorpayPaymetnLink(User user, Long amount, Long orderId)
            throws RazorpayException {

        // Always attempt exact cart amount first so live mode and high-limit accounts charge the exact total.
        long amountInPaise = amount * 100;
        log.info("Creating Razorpay payment link: orderId={} amount={} paise (Rs. {})", orderId, amountInPaise, amount);

        if (razorpayKeyId == null || razorpayKeyId.isBlank() || razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
            throw new RazorpayException("Razorpay API credentials are not configured. Please set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET.");
        }

        try {
            RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            JSONObject request = new JSONObject();
            request.put("amount", amountInPaise);
            request.put("currency", "INR");

            JSONObject customer = new JSONObject();
            customer.put("name",  user.getFullName());
            customer.put("email", user.getEmail());
            if (user.getMobile() != null) {
                customer.put("contact", user.getMobile());
            }
            request.put("customer", customer);

            JSONObject notify = new JSONObject();
            notify.put("email", true);
            request.put("notify", notify);

            // Callback URL is configurable — NOT hardcoded to localhost
            String callbackUrl = callbackBaseUrl + "/payment-success/" + orderId;
            request.put("callback_url", callbackUrl);
            request.put("callback_method", "get");

            PaymentLink paymentLink = razorpay.paymentLink.create(request);

            String paymentLinkId = paymentLink.get("id");

            PaymentOrder paymentOrder = getPaymentOrderById(orderId);
            paymentOrder.setPaymentLinkId(paymentLinkId);
            paymentOrderRepository.save(paymentOrder);

            log.info("Razorpay payment link created: linkId={} orderId={}", paymentLinkId, orderId);

            return paymentLink;

        } catch (Exception e) {
            // As an extra failsafe: if Razorpay reports amount exceeds maximum, retry with max test limit
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("amount exceeds maximum") && amountInPaise > 5000000L) {
                log.warn("Retrying Razorpay payment link creation with capped amount of 5000000 paise for orderId={}", orderId);
                try {
                    RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
                    JSONObject retryRequest = new JSONObject();
                    retryRequest.put("amount", 5000000L);
                    retryRequest.put("currency", "INR");

                    JSONObject customer = new JSONObject();
                    customer.put("name", user.getFullName());
                    customer.put("email", user.getEmail());
                    if (user.getMobile() != null) {
                        customer.put("contact", user.getMobile());
                    }
                    retryRequest.put("customer", customer);

                    JSONObject notify = new JSONObject();
                    notify.put("email", true);
                    retryRequest.put("notify", notify);

                    String callbackUrl = callbackBaseUrl + "/payment-success/" + orderId;
                    retryRequest.put("callback_url", callbackUrl);
                    retryRequest.put("callback_method", "get");

                    PaymentLink retryPaymentLink = razorpay.paymentLink.create(retryRequest);
                    String paymentLinkId = retryPaymentLink.get("id");

                    PaymentOrder paymentOrder = getPaymentOrderById(orderId);
                    paymentOrder.setPaymentLinkId(paymentLinkId);
                    paymentOrderRepository.save(paymentOrder);

                    log.info("Razorpay payment link created on retry: linkId={} orderId={}", paymentLinkId, orderId);
                    return retryPaymentLink;
                } catch (Exception retryEx) {
                    log.error("Failed on retry creating Razorpay payment link for order {}: {}", orderId, retryEx.getMessage());
                    throw new RazorpayException(retryEx.getMessage());
                }
            }

            log.error("Failed to create Razorpay payment link for order {}: {}", orderId, e.getMessage());
            throw new RazorpayException(e.getMessage());
        }
    }

    @Override
    public com.razorpay.Order createRazorpayOrder(User user, Long amount, Long orderId)
            throws RazorpayException {

        long amountInPaise = amount * 100;
        log.info("Creating Razorpay Standard Order: orderId={} amount={} paise (Rs. {})",
                orderId, amountInPaise, amount);

        try {
            RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "order_rcpt_" + orderId);

            JSONObject notes = new JSONObject();
            notes.put("userId", user.getId());
            notes.put("paymentOrderId", orderId);
            notes.put("customerEmail", user.getEmail());
            orderRequest.put("notes", notes);

            com.razorpay.Order razorpayOrder = razorpay.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");

            PaymentOrder paymentOrder = getPaymentOrderById(orderId);
            paymentOrder.setRazorpayOrderId(razorpayOrderId);
            if (paymentOrder.getPaymentLinkId() == null || paymentOrder.getPaymentLinkId().isBlank()) {
                paymentOrder.setPaymentLinkId(razorpayOrderId);
            }
            paymentOrderRepository.save(paymentOrder);

            log.info("Razorpay Standard Order created successfully: razorpayOrderId={} orderId={}",
                    razorpayOrderId, orderId);

            return razorpayOrder;

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay Standard Order for order {}: {}", orderId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating Razorpay Standard Order for order {}: {}", orderId, e.getMessage());
            throw new RazorpayException(e.getMessage());
        }
    }

    @Override
    public String getRazorpayKeyId() {
        return razorpayKeyId;
    }
}
