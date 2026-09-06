package com.zosh.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zosh.domain.OrderStatus;
import com.zosh.exceptions.InsufficientInventoryException;
import com.zosh.exceptions.InvalidOrderStateException;
import com.zosh.model.Address;
import com.zosh.model.Cart;
import com.zosh.model.CartItem;
import com.zosh.model.Order;
import com.zosh.model.Product;
import com.zosh.model.Seller;
import com.zosh.model.User;
import com.zosh.dto.pricing.ProductPricingDto;
import com.zosh.model.ProductVariant;
import com.zosh.repository.AddressRepository;
import com.zosh.repository.DealRepository;
import com.zosh.repository.OrderItemRepository;
import com.zosh.repository.OrderRepository;
import com.zosh.repository.ProductRepository;
import com.zosh.repository.ProductVariantRepository;
import com.zosh.repository.UserRepository;
import com.zosh.service.PricingService;

/**
 * Unit tests for OrderServiceImpl.
 *
 * Critical paths tested:
 * 1. Inventory deducted on order creation
 * 2. InsufficientInventoryException thrown when stock is zero
 * 3. Seller ownership check prevents IDOR on updateOrderStatus
 * 4. cancelOrder restores inventory
 * 5. Invalid state transitions are rejected
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private PricingService pricingService;
    @Mock private DealRepository dealRepository;

    @InjectMocks private OrderServiceImpl orderService;

    private User user;
    private Seller seller;
    private Product product;
    private CartItem cartItem;
    private Cart cart;
    private Address address;

    @BeforeEach
    void setUp() {
        seller = new Seller();
        seller.setId(1L);
        seller.setEmail("seller@test.com");

        product = new Product();
        product.setId(10L);
        product.setTitle("Test Product");
        product.setMrpPrice(1000);
        product.setSellingPrice(800);
        product.setQuantity(5);
        product.setSeller(seller);

        user = new User();
        user.setId(100L);
        user.setEmail("user@test.com");

        address = new Address();
        address.setId(1L);
        address.setAddress("123 Test St");
        address.setCity("Mumbai");
        address.setState("Maharashtra");
        address.setPinCode("400001");

        cartItem = new CartItem();
        cartItem.setProduct(product);
        cartItem.setQuantity(2);
        cartItem.setMrpPrice(1000);
        cartItem.setSellingPrice(800);
        cartItem.setUserId(100L);

        cart = new Cart();
        cart.setUser(user);
        cart.setCartItems(Set.of(cartItem));
    }

    // ============================================================
    // createOrder tests
    // ============================================================

    @Test
    @DisplayName("createOrder: should deduct inventory from product on success")
    void createOrder_shouldDeductInventory() {
        when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
        when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any())).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });
        when(orderItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProductPricingDto pricing = new ProductPricingDto();
        pricing.setBasePrice(800);
        pricing.setMrpPrice(1000);
        pricing.setEffectivePrice(800);
        pricing.setDiscountAmount(0);
        when(pricingService.calculateProductPricing(any(), any())).thenReturn(pricing);

        orderService.createOrder(user, address, cart);

        // CRITICAL: stock should be reduced by cartItem.quantity (2)
        assertEquals(3, product.getQuantity(),
                "Inventory should be reduced from 5 to 3 after ordering qty=2");
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("createOrder: should throw InsufficientInventoryException when stock is 0")
    void createOrder_insufficientStock_throwsException() {
        product.setQuantity(0);

        when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
        when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(product));

        InsufficientInventoryException ex = assertThrows(
                InsufficientInventoryException.class,
                () -> orderService.createOrder(user, address, cart));

        assertEquals(10L, ex.getProductId());
        assertEquals(2, ex.getRequested());
        assertEquals(0, ex.getAvailable());
    }

    @Test
    @DisplayName("createOrder: should throw when cart is empty")
    void createOrder_emptyCart_throwsException() {
        cart.setCartItems(Set.of());

        assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(user, address, cart));
    }

    // ============================================================
    // updateOrderStatus (seller-aware) tests — IDOR protection
    // ============================================================

    @Test
    @DisplayName("updateOrderStatus: seller can update their own order")
    void updateOrderStatus_ownOrder_succeeds() {
        Order order = new Order();
        order.setId(1L);
        order.setSellerId(1L); // matches seller
        order.setOrderStatus(OrderStatus.PENDING);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order updated = orderService.updateOrderStatus(1L, OrderStatus.PLACED, 1L);

        assertEquals(OrderStatus.PLACED, updated.getOrderStatus());
    }

    @Test
    @DisplayName("updateOrderStatus: should throw when seller tries to update another seller's order (IDOR)")
    void updateOrderStatus_differentSeller_throwsException() {
        Order order = new Order();
        order.setId(1L);
        order.setSellerId(99L); // belongs to different seller
        order.setOrderStatus(OrderStatus.PENDING);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // Seller ID 1 tries to update order belonging to seller ID 99 — MUST fail
        assertThrows(InvalidOrderStateException.class,
                () -> orderService.updateOrderStatus(1L, OrderStatus.PLACED, 1L));
    }

    // ============================================================
    // cancelOrder tests
    // ============================================================

    @Test
    @DisplayName("cancelOrder: should restore inventory on cancellation")
    void cancelOrder_shouldRestoreInventory() {
        product.setQuantity(3); // current stock

        com.zosh.model.OrderItem orderItem = new com.zosh.model.OrderItem();
        orderItem.setProduct(product);
        orderItem.setQuantity(2);

        Order order = new Order();
        order.setId(1L);
        order.setSellerId(1L);
        order.setUser(user);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setOrderItems(java.util.List.of(orderItem));
        order.setTotalSellingPrice(1600);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orderService.cancelOrder(1L, user);

        // Inventory should be restored: 3 + 2 = 5
        assertEquals(5, product.getQuantity(),
                "Inventory should be restored from 3 to 5 after cancellation");
    }

    @Test
    @DisplayName("cancelOrder: should restore variant inventory and sync product stock on cancellation")
    void cancelOrder_withVariant_shouldRestoreVariantAndProductInventory() {
        ProductVariant variant = new ProductVariant();
        variant.setId(50L);
        variant.setQuantity(4);
        variant.setProduct(product);

        com.zosh.model.OrderItem orderItem = new com.zosh.model.OrderItem();
        orderItem.setProduct(product);
        orderItem.setVariant(variant);
        orderItem.setQuantity(3);

        Order order = new Order();
        order.setId(2L);
        order.setSellerId(1L);
        order.setUser(user);
        order.setOrderStatus(OrderStatus.PLACED);
        order.setOrderItems(java.util.List.of(orderItem));

        when(orderRepository.findById(2L)).thenReturn(Optional.of(order));
        when(variantRepository.findByIdWithLock(50L)).thenReturn(Optional.of(variant));
        when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(product));
        when(variantRepository.findByProduct(product)).thenReturn(java.util.List.of(variant));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orderService.cancelOrder(2L, user);

        // Variant stock should be restored: 4 + 3 = 7
        assertEquals(7, variant.getQuantity(), "Variant stock should be restored from 4 to 7");
        verify(variantRepository).save(variant);
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("cancelOrder: should throw when user tries to cancel another user's order")
    void cancelOrder_differentUser_throwsException() {
        User otherUser = new User();
        otherUser.setId(999L);

        Order order = new Order();
        order.setId(1L);
        order.setUser(user); // belongs to user ID 100
        order.setOrderStatus(OrderStatus.PENDING);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // User 999 trying to cancel user 100's order
        assertThrows(InvalidOrderStateException.class,
                () -> orderService.cancelOrder(1L, otherUser));
    }

    @Test
    @DisplayName("cancelOrder: should reject cancellation of DELIVERED order")
    void cancelOrder_delivered_throwsException() {
        Order order = new Order();
        order.setId(1L);
        order.setUser(user);
        order.setOrderStatus(OrderStatus.DELIVERED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStateException.class,
                () -> orderService.cancelOrder(1L, user));
    }

    // ============================================================
    // State machine tests
    // ============================================================

    @Test
    @DisplayName("updateOrderStatus: invalid transition PENDING→DELIVERED should throw")
    void updateOrderStatus_invalidTransition_throwsException() {
        Order order = new Order();
        order.setId(1L);
        order.setSellerId(1L);
        order.setOrderStatus(OrderStatus.PENDING);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStateException.class,
                () -> orderService.updateOrderStatus(1L, OrderStatus.DELIVERED, 1L));
    }
}
