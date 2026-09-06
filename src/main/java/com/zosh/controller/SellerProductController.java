package com.zosh.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zosh.exceptions.ProductException;
import com.zosh.exceptions.SellerException;
import com.zosh.model.Product;
import com.zosh.model.Seller;
import com.zosh.request.CreateProductRequest;
import com.zosh.service.ProductService;
import com.zosh.service.SellerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/sellers/products")
@Tag(name = "Seller - Products", description = "Endpoints for sellers to manage their own products")
@PreAuthorize("hasRole('SELLER')")
public class SellerProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private SellerService sellerService;

    @GetMapping
    @Operation(summary = "Get all products belonging to the authenticated seller")
    public ResponseEntity<List<Product>> getProductBySellerId(
            @RequestHeader("Authorization") String jwt) {
        Seller seller = sellerService.getSellerProfile(jwt);
        List<Product> products = productService.getProductsBySellerId(seller.getId());
        return ResponseEntity.ok(products);
    }

    @PostMapping
    @Operation(summary = "Create a new product under the authenticated seller")
    public ResponseEntity<Product> createProduct(
            @Valid @RequestBody CreateProductRequest req,
            @RequestHeader("Authorization") String jwt) throws SellerException {
        Seller seller = sellerService.getSellerProfile(jwt);
        sellerService.verifySellerIsActive(seller);

        Product product = productService.createProduct(req, seller);
        return new ResponseEntity<>(product, HttpStatus.CREATED);
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Delete a product (enforces seller ownership check)")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long productId,
            @RequestHeader("Authorization") String jwt) {
        Seller seller = sellerService.getSellerProfile(jwt);
        productService.deleteProduct(productId, seller.getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Update a product (enforces seller ownership check)")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long productId,
            @RequestBody Product product,
            @RequestHeader("Authorization") String jwt) {
        Seller seller = sellerService.getSellerProfile(jwt);
        Product updatedProduct = productService.updateProduct(productId, product, seller.getId());
        return ResponseEntity.ok(updatedProduct);
    }
}
