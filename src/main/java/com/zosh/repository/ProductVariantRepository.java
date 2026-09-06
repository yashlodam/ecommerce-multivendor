package com.zosh.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.zosh.model.Product;
import com.zosh.model.ProductVariant;

import jakarta.persistence.LockModeType;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    /** All variants for a given product */
    List<ProductVariant> findByProduct(Product product);

    /** All variants by product ID */
    List<ProductVariant> findByProductId(Long productId);

    /** Find the default variant for a product (used for products without real variants) */
    Optional<ProductVariant> findByProductAndIsDefaultTrue(Product product);

    /** Look up a specific variant by product + name (used during migration dedup check) */
    Optional<ProductVariant> findByProductAndVariantName(Product product, String variantName);

    /**
     * Acquire a pessimistic write lock on the variant row.
     * Used in checkout to prevent overselling race conditions:
     * two concurrent transactions will serialize here; the second
     * will wait until the first commits or rolls back.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM ProductVariant v WHERE v.id = :id")
    Optional<ProductVariant> findByIdWithLock(@Param("id") Long id);

    /** Check whether any variants exist for a product (used by migration service) */
    boolean existsByProduct(Product product);

    /** Count variants for a product */
    long countByProduct(Product product);
}
