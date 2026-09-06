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

import com.zosh.model.ProductVariant;
import com.zosh.model.Seller;
import com.zosh.request.ProductVariantRequest;
import com.zosh.service.ProductService;
import com.zosh.service.SellerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Public read + seller-scoped write endpoints for product variants.
 *
 * Public:
 *   GET  /products/{productId}/variants          — list all variants for a product
 *
 * Seller-authenticated (requires JWT with ROLE_SELLER):
 *   POST   /sellers/products/{productId}/variants  — create a new variant
 *   PUT    /sellers/products/variants/{variantId}  — update a variant
 *   DELETE /sellers/products/variants/{variantId}  — delete a variant
 */
@RestController
@Tag(name = "Product Variants", description = "Endpoints for managing product size/storage/option variants")
public class ProductVariantController {

    @Autowired
    private ProductService productService;

    @Autowired
    private SellerService sellerService;

    // ─── Public ────────────────────────────────────────────────────────────────

    @GetMapping("/products/{productId}/variants")
    @Operation(summary = "List all variants for a product (public)")
    public ResponseEntity<List<ProductVariant>> getVariants(@PathVariable Long productId) {
        List<ProductVariant> variants = productService.getVariantsByProductId(productId);
        return ResponseEntity.ok(variants);
    }

    // ─── Seller-Authenticated ──────────────────────────────────────────────────

    @PostMapping("/sellers/products/{productId}/variants")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Create a new variant for a seller's product")
    public ResponseEntity<ProductVariant> createVariant(
            @PathVariable Long productId,
            @Valid @RequestBody ProductVariantRequest req,
            @RequestHeader("Authorization") String jwt) {

        Seller seller = sellerService.getSellerProfile(jwt);
        ProductVariant variant = productService.createVariant(productId, req, seller.getId());
        return new ResponseEntity<>(variant, HttpStatus.CREATED);
    }

    @PutMapping("/sellers/products/variants/{variantId}")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Update a variant (seller must own the product)")
    public ResponseEntity<ProductVariant> updateVariant(
            @PathVariable Long variantId,
            @Valid @RequestBody ProductVariantRequest req,
            @RequestHeader("Authorization") String jwt) {

        Seller seller = sellerService.getSellerProfile(jwt);
        ProductVariant variant = productService.updateVariant(variantId, req, seller.getId());
        return ResponseEntity.ok(variant);
    }

    @DeleteMapping("/sellers/products/variants/{variantId}")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Delete a variant (seller must own the product, product must have > 1 variant)")
    public ResponseEntity<Void> deleteVariant(
            @PathVariable Long variantId,
            @RequestHeader("Authorization") String jwt) {

        Seller seller = sellerService.getSellerProfile(jwt);
        productService.deleteVariant(variantId, seller.getId());
        return ResponseEntity.noContent().build();
    }
}
