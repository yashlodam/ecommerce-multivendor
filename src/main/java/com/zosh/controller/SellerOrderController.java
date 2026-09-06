package com.zosh.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zosh.domain.OrderStatus;
import com.zosh.model.Order;
import com.zosh.model.Seller;
import com.zosh.service.OrderService;
import com.zosh.service.SellerService;

/**
 * Order management endpoints for authenticated sellers.
 *
 * SECURITY: All endpoints verify the requesting seller's JWT and validate that
 * they can only operate on orders belonging to their own seller account.
 * This prevents IDOR (Insecure Direct Object Reference) attacks where one
 * seller could manipulate another seller's orders.
 */
@RestController
@RequestMapping("/seller/orders")
@PreAuthorize("hasRole('SELLER')")
public class SellerOrderController {

    @Autowired private OrderService orderService;
    @Autowired private SellerService sellerService;

    /**
     * Returns all orders for the authenticated seller.
     */
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrdersForSeller(
            @RequestHeader("Authorization") String jwt) {

        Seller seller = sellerService.getSellerProfile(jwt);
        List<Order> orders = orderService.sellersOrder(seller.getId());

        return ResponseEntity.ok(orders);
    }

    /**
     * Updates the status of an order — ONLY if the order belongs to the authenticated seller.
     *
     * SECURITY FIX: Previously, any seller could update any order by knowing its ID (IDOR).
     * Now we pass the seller's ID to updateOrderStatus() which validates ownership.
     */
    @PatchMapping("/{orderId}/status/{orderStatus}")
    public ResponseEntity<Order> updateOrderStatus(
            @RequestHeader("Authorization") String jwt,
            @PathVariable Long orderId,
            @PathVariable OrderStatus orderStatus) {

        Seller seller = sellerService.getSellerProfile(jwt);

        // CRITICAL: pass sellerId — service validates the seller owns this order
        Order order = orderService.updateOrderStatus(orderId, orderStatus, seller.getId());

        return ResponseEntity.ok(order);
    }
}
