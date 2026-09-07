package com.zosh.repository;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.zosh.model.Deal;
import com.zosh.model.HomeCategory;

public interface DealRepository extends JpaRepository<Deal, Long> {

    Optional<Deal> findByCategory(HomeCategory category);

    Optional<Deal> findByCategoryId(Long categoryId);

    List<Deal> findBySellerId(Long sellerId);

    List<Deal> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    @Query("SELECT d FROM Deal d WHERE (d.active IS NULL OR d.active = true) AND (d.startAt IS NULL OR d.startAt <= :now) AND (d.endAt IS NULL OR d.endAt >= :now) AND (d.usageLimit IS NULL OR d.usageCount IS NULL OR d.usageCount < d.usageLimit)")
    List<Deal> findAllCurrentlyActive(@Param("now") LocalDateTime now);

    @Query("SELECT DISTINCT d FROM Deal d LEFT JOIN d.products p WHERE (d.active IS NULL OR d.active = true) AND (d.startAt IS NULL OR d.startAt <= :now) AND (d.endAt IS NULL OR d.endAt >= :now) AND (d.usageLimit IS NULL OR d.usageCount IS NULL OR d.usageCount < d.usageLimit) AND (d.dealType = com.zosh.domain.DealType.PRODUCT AND p.id = :productId)")
    List<Deal> findActiveProductDeals(@Param("productId") Long productId, @Param("now") LocalDateTime now);

    @Query("SELECT d FROM Deal d WHERE (d.active IS NULL OR d.active = true) AND (d.startAt IS NULL OR d.startAt <= :now) AND (d.endAt IS NULL OR d.endAt >= :now) AND (d.usageLimit IS NULL OR d.usageCount IS NULL OR d.usageCount < d.usageLimit) AND d.dealType = com.zosh.domain.DealType.CATEGORY AND (LOWER(d.categorySlug) = LOWER(:categorySlug) OR (d.category IS NOT NULL AND LOWER(d.category.categoryId) = LOWER(:categorySlug)))")
    List<Deal> findActiveCategoryDeals(@Param("categorySlug") String categorySlug, @Param("now") LocalDateTime now);

    @Query("SELECT d FROM Deal d WHERE (d.active IS NULL OR d.active = true) AND (d.startAt IS NULL OR d.startAt <= :now) AND (d.endAt IS NULL OR d.endAt >= :now) AND (d.usageLimit IS NULL OR d.usageCount IS NULL OR d.usageCount < d.usageLimit) AND d.dealType = com.zosh.domain.DealType.SELLER AND d.seller.id = :sellerId")
    List<Deal> findActiveSellerDeals(@Param("sellerId") Long sellerId, @Param("now") LocalDateTime now);

    @Query("SELECT d FROM Deal d WHERE (d.active IS NULL OR d.active = true) AND (d.startAt IS NULL OR d.startAt <= :now) AND (d.endAt IS NULL OR d.endAt >= :now) AND (d.usageLimit IS NULL OR d.usageCount IS NULL OR d.usageCount < d.usageLimit) AND d.dealType = com.zosh.domain.DealType.ORDER")
    List<Deal> findActiveOrderDeals(@Param("now") LocalDateTime now);

    @Query("SELECT d FROM Deal d WHERE (d.active IS NULL OR d.active = true) AND (d.startAt IS NULL OR d.startAt <= :now) AND (d.endAt IS NULL OR d.endAt >= :now) AND (d.usageLimit IS NULL OR d.usageCount IS NULL OR d.usageCount < d.usageLimit) AND d.dealType = com.zosh.domain.DealType.CATEGORY")
    List<Deal> findAllActiveCategoryDeals(@Param("now") LocalDateTime now);
}

