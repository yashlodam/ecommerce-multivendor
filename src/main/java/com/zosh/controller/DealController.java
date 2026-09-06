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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zosh.dto.deal.DealRequest;
import com.zosh.dto.deal.DealResponse;
import com.zosh.response.ApiResponse;
import com.zosh.service.DealService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/deals")
@Tag(name = "Admin - Deals", description = "Administrative deal management endpoints across all deal types")
@PreAuthorize("hasRole('ADMIN')")
public class DealController {

    @Autowired
    private DealService dealService;

    @GetMapping
    @Operation(summary = "Get all promotional deals (Admin)")
    public ResponseEntity<List<DealResponse>> getDeals() {
        List<DealResponse> deals = dealService.getAllDealsAdmin();
        return ResponseEntity.ok(deals);
    }

    @PostMapping
    @Operation(summary = "Create a promotional deal across Products, Categories, Sellers, or Orders (Admin)")
    public ResponseEntity<DealResponse> createDeals(@Valid @RequestBody DealRequest req) {
        DealResponse createdDeal = dealService.createDealFromRequest(req, null, true);
        return new ResponseEntity<>(createdDeal, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get deal details by ID (Admin)")
    public ResponseEntity<DealResponse> getDealById(@PathVariable Long id) {
        DealResponse deal = dealService.getDealById(id, null, true);
        return ResponseEntity.ok(deal);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a promotional deal by ID (Admin)")
    public ResponseEntity<DealResponse> updateDeal(
            @PathVariable Long id,
            @Valid @RequestBody DealRequest req) {
        DealResponse updatedDeal = dealService.updateDealFromRequest(id, req, null, true);
        return ResponseEntity.ok(updatedDeal);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update a promotional deal by ID (Admin)")
    public ResponseEntity<DealResponse> patchDeal(
            @PathVariable Long id,
            @RequestBody DealRequest req) {
        DealResponse updatedDeal = dealService.updateDealFromRequest(id, req, null, true);
        return ResponseEntity.ok(updatedDeal);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Toggle active/inactive status of a deal (Admin)")
    public ResponseEntity<DealResponse> toggleDealStatus(@PathVariable Long id) {
        DealResponse updatedDeal = dealService.toggleDealStatus(id, null, true);
        return ResponseEntity.ok(updatedDeal);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a promotional deal by ID (Admin)")
    public ResponseEntity<ApiResponse> deleteDeals(@PathVariable Long id) {
        dealService.deleteDealWithAuth(id, null, true);
        ApiResponse res = new ApiResponse("Deal deleted successfully");
        return ResponseEntity.ok(res);
    }
}
