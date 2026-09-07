package com.zosh.controller;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;
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
import com.zosh.exceptions.ResourceNotFoundException;
import com.zosh.exceptions.SellerException;
import com.zosh.model.PaymentOrder;
import com.zosh.model.User;
import com.zosh.response.ApiResponse;
import com.zosh.service.PaymentService;
import com.zosh.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
@RequestMapping("/api/payment")
@Tag(name = "Payment Verification", description = "Payment webhook callback and transaction verification endpoints")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserService userService;

    @GetMapping("/{paymentId}")
    @Operation(summary = "Verify online payment completion with payment gateway (Razorpay)")
    public ResponseEntity<ApiResponse> paymentSuccessHandler(
            @PathVariable String paymentId,
            @RequestParam(required = false) String paymentLinkId,
            @RequestHeader("Authorization") String jwt) throws RazorpayException, SellerException {

        User user = userService.findUserByJwtToken(jwt);

        PaymentOrder paymentOrder = null;

        // 1. Try resolving using paymentLinkId if provided (supports razorpay order_..., plink_..., or numeric id)
        if (paymentLinkId != null && !paymentLinkId.isBlank()) {
            try {
                paymentOrder = paymentService.getPaymentOrderByPaymentId(paymentLinkId);
            } catch (Exception e) {
                log.warn("PaymentOrder lookup by identifier '{}' failed: {}. Attempting fallback via Razorpay payment ID.",
                        paymentLinkId, e.getMessage());
            }
        }

        // 2. Resilient fallback: fetch the payment entity directly from Razorpay to extract order_id or notes
        if (paymentOrder == null) {
            paymentOrder = paymentService.resolvePaymentOrderFromRazorpayPayment(paymentId);
        }

        // 3. If still not found, throw descriptive ResourceNotFoundException
        if (paymentOrder == null) {
            throw new ResourceNotFoundException("PaymentOrder", "paymentLinkId / paymentId",
                    (paymentLinkId != null ? paymentLinkId : paymentId));
        }

        boolean paymentSuccess = paymentService.ProceedPaymentOrder(
                paymentOrder,
                paymentId,
                paymentLinkId != null ? paymentLinkId : paymentOrder.getPaymentLinkId());

        if (paymentSuccess) {
            ApiResponse res = new ApiResponse("Payment successful and orders confirmed");
            return ResponseEntity.ok(res);
        }

        ApiResponse res = new ApiResponse("Payment verification failed");
        return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
    }
}
