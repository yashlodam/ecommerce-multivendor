package com.zosh.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zosh.dto.deal.DealRequest;
import com.zosh.dto.deal.DealResponse;
import com.zosh.exceptions.SellerException;
import com.zosh.model.Seller;
import com.zosh.response.ApiResponse;
import com.zosh.service.DealService;
import com.zosh.service.SellerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/sellers/deals")
@Tag(name = "Seller - Deals", description = "Endpoints for sellers to manage promotions on their own products")
@PreAuthorize("hasRole('SELLER')")
public class SellerDealController {

    @Autowired
    private DealService dealService;

    @Autowired
    private SellerService sellerService;

    @GetMapping
    @Operation(summary = "Get all promotional deals created by the authenticated seller")
    public ResponseEntity<List<DealResponse>> getSellerDeals(
            @RequestHeader("Authorization") String jwt) {
        Seller seller = sellerService.getSellerProfile(jwt);
        List<DealResponse> deals = dealService.getSellerDeals(seller.getId());
        return ResponseEntity.ok(deals);
    }

    @PostMapping
    @Operation(summary = "Create a product-level or store deal (restricted to seller's owned products)")
    public ResponseEntity<DealResponse> createDeal(
            @Valid @RequestBody DealRequest req,
            @RequestHeader("Authorization") String jwt) throws SellerException {
        Seller seller = sellerService.getSellerProfile(jwt);
        sellerService.verifySellerIsActive(seller);

        DealResponse created = dealService.createDealFromRequest(req, seller.getId(), false);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a specific deal by ID (restricted to seller's owned deal)")
    public ResponseEntity<DealResponse> getDealById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) {
        Seller seller = sellerService.getSellerProfile(jwt);
        DealResponse deal = dealService.getDealById(id, seller.getId(), false);
        return ResponseEntity.ok(deal);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing promotional deal (restricted to seller's owned deal)")
    public ResponseEntity<DealResponse> updateDeal(
            @PathVariable Long id,
            @Valid @RequestBody DealRequest req,
            @RequestHeader("Authorization") String jwt) throws SellerException {
        Seller seller = sellerService.getSellerProfile(jwt);
        sellerService.verifySellerIsActive(seller);

        DealResponse updated = dealService.updateDealFromRequest(id, req, seller.getId(), false);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Toggle active/inactive status of a deal")
    public ResponseEntity<DealResponse> toggleDealStatus(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws SellerException {
        Seller seller = sellerService.getSellerProfile(jwt);
        sellerService.verifySellerIsActive(seller);

        DealResponse updated = dealService.toggleDealStatus(id, seller.getId(), false);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a promotional deal (restricted to seller's owned deal)")
    public ResponseEntity<ApiResponse> deleteDeal(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws SellerException {
        Seller seller = sellerService.getSellerProfile(jwt);
        sellerService.verifySellerIsActive(seller);

        dealService.deleteDealWithAuth(id, seller.getId(), false);
        return ResponseEntity.ok(new ApiResponse("Deal deleted successfully"));
    }
}
