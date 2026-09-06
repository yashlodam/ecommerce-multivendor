package com.zosh.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zosh.model.Product;
import com.zosh.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/products")
@Tag(name = "Products - Public Catalog", description = "Public endpoints for browsing, filtering, searching, and viewing products")
public class ProductController {

    @Autowired
    private ProductService productservice;

    @GetMapping("/{productId:[0-9]+}")
    @Operation(summary = "Get product details by product ID")
    public ResponseEntity<Product> getProductById(@PathVariable Long productId) {
        Product product = productservice.findProductById(productId);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/search")
    @Operation(summary = "Search products by keyword/title")
    public ResponseEntity<List<Product>> searchProduct(@RequestParam(required = false) String query) {
        List<Product> products = productservice.searchProducts(query);
        return ResponseEntity.ok(products);
    }

    @GetMapping
    @Operation(summary = "Get paginated, filtered, and sorted products catalog")
    public ResponseEntity<Page<Product>> getAllProducts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String colors,
            @RequestParam(required = false) String sizes,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Integer minDiscount,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String stock,
            @RequestParam(defaultValue = "0") Integer pageNumber) {

        Page<Product> products = productservice.getAllProducts(
                query,
                category,
                brand,
                colors,
                sizes,
                minPrice,
                maxPrice,
                minDiscount,
                sort,
                stock,
                pageNumber
        );

        return ResponseEntity.ok(products);
    }

    @GetMapping("/brands")
    @Operation(summary = "Get distinct real brands available in products catalog")
    public ResponseEntity<List<String>> getDistinctBrands(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String query) {
        List<String> brands = productservice.getDistinctBrands(category, query);
        return ResponseEntity.ok(brands);
    }
}
