package com.zosh.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zosh.domain.PaymentOrderStatus;
import com.zosh.exceptions.ResourceNotFoundException;
import com.zosh.model.PaymentOrder;
import com.zosh.repository.AddressRepository;
import com.zosh.repository.OrderRepository;
import com.zosh.repository.PaymentOrderRepository;
import com.zosh.repository.UserRepository;
import com.zosh.service.CartService;
import com.zosh.service.NotificationService;
import com.zosh.service.OrderService;
import com.zosh.service.SellerReportService;
import com.zosh.service.SellerService;
import com.zosh.service.TransactionService;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private PaymentOrderRepository paymentOrderRepository;
    @Mock private CartService cartService;
    @Mock private OrderRepository orderRepository;
    @Mock private SellerService sellerService;
    @Mock private SellerReportService reportService;
    @Mock private TransactionService transactionService;
    @Mock private OrderService orderService;
    @Mock private AddressRepository addressRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private PaymentOrder testPaymentOrder;

    @BeforeEach
    void setUp() {
        testPaymentOrder = new PaymentOrder();
        testPaymentOrder.setId(2L);
        testPaymentOrder.setAmount(51299L);
        testPaymentOrder.setStatus(PaymentOrderStatus.PENDING);
        testPaymentOrder.setRazorpayOrderId("order_TZ7YpgKgd3zCdp");
        testPaymentOrder.setPaymentLinkId("plink_TZ7YqdGg7mrRDM");
    }

    @Test
    @DisplayName("Should resolve PaymentOrder by razorpayOrderId when identifier starts with order_")
    void testGetPaymentOrderByRazorpayOrderId() {
        when(paymentOrderRepository.findByRazorpayOrderId("order_TZ7YpgKgd3zCdp"))
                .thenReturn(testPaymentOrder);

        PaymentOrder result = paymentService.getPaymentOrderByPaymentId("order_TZ7YpgKgd3zCdp");

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("order_TZ7YpgKgd3zCdp", result.getRazorpayOrderId());
        verify(paymentOrderRepository).findByRazorpayOrderId("order_TZ7YpgKgd3zCdp");
    }

    @Test
    @DisplayName("Should resolve PaymentOrder by paymentLinkId when identifier starts with plink_")
    void testGetPaymentOrderByPaymentLinkId() {
        when(paymentOrderRepository.findByPaymentLinkId("plink_TZ7YqdGg7mrRDM"))
                .thenReturn(testPaymentOrder);

        PaymentOrder result = paymentService.getPaymentOrderByPaymentId("plink_TZ7YqdGg7mrRDM");

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("plink_TZ7YqdGg7mrRDM", result.getPaymentLinkId());
        verify(paymentOrderRepository).findByPaymentLinkId("plink_TZ7YqdGg7mrRDM");
    }

    @Test
    @DisplayName("Should resolve PaymentOrder by numeric ID fallback when ID is passed")
    void testGetPaymentOrderByNumericIdFallback() {
        when(paymentOrderRepository.findByPaymentLinkId("2")).thenReturn(null);
        when(paymentOrderRepository.findByRazorpayOrderId("2")).thenReturn(null);
        when(paymentOrderRepository.findById(2L)).thenReturn(Optional.of(testPaymentOrder));

        PaymentOrder result = paymentService.getPaymentOrderByPaymentId("2");

        assertNotNull(result);
        assertEquals(2L, result.getId());
        verify(paymentOrderRepository).findById(2L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when payment order identifier does not exist")
    void testGetPaymentOrderNotFound() {
        when(paymentOrderRepository.findByRazorpayOrderId("order_nonexistent")).thenReturn(null);
        when(paymentOrderRepository.findByPaymentLinkId("order_nonexistent")).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.getPaymentOrderByPaymentId("order_nonexistent"));
    }

    @Test
    @DisplayName("resolvePaymentOrderFromRazorpayPayment should return null if credentials not set or paymentId blank")
    void testResolvePaymentOrderGracefulNull() {
        PaymentOrder resultBlank = paymentService.resolvePaymentOrderFromRazorpayPayment("");
        assertNull(resultBlank);

        PaymentOrder resultNull = paymentService.resolvePaymentOrderFromRazorpayPayment(null);
        assertNull(resultNull);
    }
}