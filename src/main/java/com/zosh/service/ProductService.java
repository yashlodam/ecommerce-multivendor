package com.zosh.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.zosh.model.Product;
import com.zosh.model.ProductVariant;
import com.zosh.model.Seller;
import com.zosh.request.CreateProductRequest;
import com.zosh.request.ProductVariantRequest;

public interface ProductService {

    // ─── Product CRUD ──────────────────────────────────────────────
    Product createProduct(CreateProductRequest req, Seller seller);

    Product findProductById(Long id);

    Page<Product> getAllProducts(
            String query,
            String category,
            String brand,
            String colors,
            String sizes,
            Integer minPrice,
            Integer maxPrice,
            Integer minDiscount,
            String sort,
            String stock,
            Integer pageNumber);

    List<Product> getProductsBySellerId(Long sellerId);

    List<Product> getFeaturedProducts();

    List<Product> getRelatedProducts(Long productId);

    // ─── Search ────────────────────────────────────────────────────
    List<Product> searchProducts(String query);

    List<Product> searchProductsByCategory(String category);

    List<Product> searchProductsByBrand(String brand);

    // ─── Product Update ────────────────────────────────────────────
    Product updateProduct(Long id, CreateProductRequest req);

    Product updateProduct(Long id, Product updatedProduct);

    Product updateProduct(Long id, Product updatedProduct, Long sellerId);

    Product updateProductStatus(Long id);

    Product updateProductStock(Long productId, Integer quantity);

    Product updateProductPrice(Long productId, Integer price);

    Product updateProductDiscount(Long productId, Integer discountPercent);

    // ─── Product Delete ────────────────────────────────────────────
    void deleteProduct(Long id);

    void deleteProduct(Long id, Long sellerId);

    // ─── Inventory ─────────────────────────────────────────────────
    boolean isProductInStock(Long productId);

    Integer getAvailableQuantity(Long productId);

    // ─── Seller Analytics ──────────────────────────────────────────
    Long getTotalProductsBySeller(Long sellerId);

    List<Product> getActiveProductsBySeller(Long sellerId);

    List<Product> getInactiveProductsBySeller(Long sellerId);

    // ─── Admin Operations ──────────────────────────────────────────
    List<Product> getPendingProducts();

    List<Product> getApprovedProducts();

    Product approveProduct(Long productId);

    Product rejectProduct(Long productId);

    // ─── Platform Analytics ────────────────────────────────────────
    Long getTotalProducts();

    Long getTotalActiveProducts();

    Long getOutOfStockProductsCount();

    // ─── Variant CRUD ──────────────────────────────────────────────

    /** Get all variants for a product */
    List<ProductVariant> getVariantsByProductId(Long productId);

    /**
     * Create a new variant for a product (seller-scoped — sellerId enforces ownership).
     */
    ProductVariant createVariant(Long productId, ProductVariantRequest req, Long sellerId);

    /**
     * Update an existing variant (seller-scoped — sellerId enforces ownership).
     */
    ProductVariant updateVariant(Long variantId, ProductVariantRequest req, Long sellerId);

    /**
     * Delete a variant (seller-scoped — sellerId enforces ownership).
     * Throws if only one variant remains (a product must always have at least one).
     */
    void deleteVariant(Long variantId, Long sellerId);

    // ─── Real Brand Discovery ──────────────────────────────────────

    /**
     * Get all distinct, non-empty brand names in the product catalog.
     */
    List<String> getAllBrands();

    /**
     * Get distinct brand names scoped by category and/or keyword query.
     */
    List<String> getDistinctBrands(String category, String query);
}