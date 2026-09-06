package com.zosh.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.zosh.model.Product;

import jakarta.persistence.LockModeType;

public interface ProductRepository
        extends JpaRepository<Product, Long>,
                JpaSpecificationExecutor<Product> {

    List<Product> findBySellerId(Long id);

    /**
     * Fetches a product with a PESSIMISTIC_WRITE lock (SELECT FOR UPDATE).
     *
     * Use this method during checkout to prevent concurrent buyers from
     * both successfully purchasing the same last item (overselling).
     *
     * Must be called within a @Transactional method.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);

    /**
     * Finds all distinct, non-empty, trimmed brand names of actual catalog products.
     */
    @Query("SELECT DISTINCT TRIM(p.brand) FROM Product p WHERE p.brand IS NOT NULL AND TRIM(p.brand) <> '' ORDER BY TRIM(p.brand) ASC")
    List<String> findDistinctBrands();

    /**
     * Finds distinct brand names within a specific category and its category hierarchy.
     */
    @Query("SELECT DISTINCT TRIM(p.brand) FROM Product p " +
           "LEFT JOIN p.category c " +
           "LEFT JOIN c.parentCategory p1 " +
           "LEFT JOIN p1.parentCategory p2 " +
           "WHERE (c.categoryId = :categoryId OR p1.categoryId = :categoryId OR p2.categoryId = :categoryId) " +
           "AND p.brand IS NOT NULL AND TRIM(p.brand) <> '' ORDER BY TRIM(p.brand) ASC")
    List<String> findDistinctBrandsByCategory(@Param("categoryId") String categoryId);

    /**
     * Finds distinct brand names matching a keyword or title query.
     */
    @Query("SELECT DISTINCT TRIM(p.brand) FROM Product p " +
           "WHERE p.brand IS NOT NULL AND TRIM(p.brand) <> '' " +
           "AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY TRIM(p.brand) ASC")
    List<String> findDistinctBrandsByQuery(@Param("query") String query);

    /**
     * Finds distinct brand names within a set of resolved category IDs.
     */
    @Query("SELECT DISTINCT TRIM(p.brand) FROM Product p " +
           "WHERE p.category.id IN :categoryIds " +
           "AND p.brand IS NOT NULL AND TRIM(p.brand) <> '' ORDER BY TRIM(p.brand) ASC")
    List<String> findDistinctBrandsByCategoryIds(@Param("categoryIds") java.util.Collection<Long> categoryIds);

    /**
     * Finds distinct brand names within a set of resolved category IDs matching a query.
     */
    @Query("SELECT DISTINCT TRIM(p.brand) FROM Product p " +
           "WHERE p.category.id IN :categoryIds " +
           "AND p.brand IS NOT NULL AND TRIM(p.brand) <> '' " +
           "AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY TRIM(p.brand) ASC")
    List<String> findDistinctBrandsByCategoryIdsAndQuery(@Param("categoryIds") java.util.Collection<Long> categoryIds, @Param("query") String query);

    /**
     * Finds distinct brand names matching both category hierarchy and keyword query.
     */
    @Query("SELECT DISTINCT TRIM(p.brand) FROM Product p " +
           "LEFT JOIN p.category c " +
           "LEFT JOIN c.parentCategory p1 " +
           "LEFT JOIN p1.parentCategory p2 " +
           "WHERE (c.categoryId = :categoryId OR p1.categoryId = :categoryId OR p2.categoryId = :categoryId) " +
           "AND p.brand IS NOT NULL AND TRIM(p.brand) <> '' " +
           "AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY TRIM(p.brand) ASC")
    List<String> findDistinctBrandsByCategoryAndQuery(@Param("categoryId") String categoryId, @Param("query") String query);
}
