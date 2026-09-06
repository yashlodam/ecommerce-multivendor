package com.zosh.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.zosh.model.Product;
import com.zosh.model.Wishlist;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    Wishlist findByUserId(Long userId);

    List<Wishlist> findByProductsContaining(Product product);
}
