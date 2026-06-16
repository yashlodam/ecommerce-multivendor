package com.zosh.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.zosh.model.Product;
import com.zosh.model.Seller;
import com.zosh.request.CreateProductRequest;

public interface ProductService {

    // Create
    Product createProduct(CreateProductRequest req, Seller seller);

    // Read
    Product findProductById(Long id);

    Page<Product> getAllProducts(
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

    // Search
    List<Product> searchProducts(String query);

    List<Product> searchProductsByCategory(String category);

    List<Product> searchProductsByBrand(String brand);

    // Update
    Product updateProduct(Long id, CreateProductRequest req);

    Product updateProductStatus(Long id);

    Product updateProductStock(Long productId, Integer quantity);

    Product updateProductPrice(Long productId, Integer price);

    Product updateProductDiscount(Long productId, Integer discountPercent);

    // Delete
    void deleteProduct(Long id);

    // Inventory
    boolean isProductInStock(Long productId);

    Integer getAvailableQuantity(Long productId);

    // Seller Operations
    Long getTotalProductsBySeller(Long sellerId);

    List<Product> getActiveProductsBySeller(Long sellerId);

    List<Product> getInactiveProductsBySeller(Long sellerId);

    // Admin Operations
    List<Product> getPendingProducts();

    List<Product> getApprovedProducts();

    Product approveProduct(Long productId);

    Product rejectProduct(Long productId);

    // Analytics
    Long getTotalProducts();

    Long getTotalActiveProducts();

    Long getOutOfStockProductsCount();

	Product updateProduct(Long id, Product updatedProduct);
}