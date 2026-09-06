package com.zosh.service;

import java.util.List;
import java.util.Set;

import com.zosh.domain.OrderStatus;
import com.zosh.model.Address;
import com.zosh.model.Cart;
import com.zosh.model.Order;
import com.zosh.model.OrderItem;
import com.zosh.model.User;

public interface OrderService {

    /** Creates per-vendor sub-orders from cart. Deducts inventory atomically. */
    Set<Order> createOrder(User user, Address shippingAddress, Cart cart);

    Order findOrderById(Long id);

    List<Order> usersOrderHistory(Long userId);

    List<Order> sellersOrder(Long sellerId);

    /**
     * Updates order status with seller ownership validation (IDOR protection).
     * Sellers may only update their own orders.
     */
    Order updateOrderStatus(Long orderId, OrderStatus orderStatus, Long sellerId);

    /**
     * Admin-only status update — bypasses seller ownership check.
     */
    Order updateOrderStatus(Long orderId, OrderStatus orderStatus);

    /**
     * Cancels an order. Validates user ownership and state transitions.
     * Restores inventory on cancellation.
     */
    Order cancelOrder(Long orderId, User user);

    OrderItem getOrderById(Long id);
}
