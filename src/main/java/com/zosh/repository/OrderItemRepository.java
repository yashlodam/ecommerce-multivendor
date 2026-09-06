package com.zosh.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.zosh.model.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    boolean existsByProductId(Long productId);

    List<OrderItem> findByUserId(Long userId);
}
