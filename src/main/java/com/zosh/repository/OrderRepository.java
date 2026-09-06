package com.zosh.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.zosh.model.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);

    List<Order> findByUserIdOrderByOrderDateDesc(Long userId);

    List<Order> findBySellerId(Long sellerId);

    List<Order> findBySellerIdOrderByOrderDateDesc(Long sellerId);

    Optional<Order> findByOrderId(String orderId);
}
