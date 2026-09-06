package com.zosh.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zosh.dto.deal.DealResponse;
import com.zosh.dto.pricing.ProductPricingDto;
import com.zosh.model.Product;
import com.zosh.model.ProductVariant;
import com.zosh.service.DealService;
import com.zosh.service.PricingService;
import com.zosh.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/deals")
@Tag(name = "Deals - Public", description = "Public deal discovery and real-time pricing calculation")
public class PublicDealController {

    @Autowired
    private DealService dealService;

    @Autowired
    private PricingService pricingService;

    @Autowired
    private ProductService productService;

    @GetMapping("/active")
    @Operation(summary = "Get all currently active promotional deals across the marketplace")
    public ResponseEntity<List<DealResponse>> getActiveDeals() {
        List<DealResponse> activeDeals = dealService.getActivePublicDeals();
        return ResponseEntity.ok(activeDeals);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get deal details by ID")
    public ResponseEntity<DealResponse> getDealById(@PathVariable Long id) {
        DealResponse deal = dealService.getDealById(id, null, true);
        return ResponseEntity.ok(deal);
    }

    @GetMapping("/pricing")
    @Operation(summary = "Calculate real-time pricing breakdown for a product applying best eligible deal")
    public ResponseEntity<ProductPricingDto> getProductPricing(
            @RequestParam Long productId,
            @RequestParam(required = false) Long variantId) {
        Product product = productService.findProductById(productId);
        ProductVariant variant = null;
        if (variantId != null && product.getVariants() != null) {
            variant = product.getVariants().stream()
                    .filter(v -> v.getId().equals(variantId))
                    .findFirst()
                    .orElse(null);
        }
        ProductPricingDto pricing = pricingService.calculateProductPricing(product, variant);
        return ResponseEntity.ok(pricing);
    }
}
