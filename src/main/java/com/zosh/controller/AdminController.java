package com.zosh.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zosh.domain.AccountStatus;
import com.zosh.exceptions.SellerException;
import com.zosh.model.Seller;
import com.zosh.model.User;
import com.zosh.response.ApiResponse;
import com.zosh.service.SellerService;
import com.zosh.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin - Platform Governance", description = "Endpoints for platform administrators to manage sellers, users, and marketplace governance")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private SellerService sellerService;

    @Autowired
    private UserService userService;

    // ================= USER MANAGEMENT =================

    @GetMapping("/users")
    @Operation(summary = "Get all registered users on the platform")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get user details by ID")
    public ResponseEntity<User> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.findUserById(userId));
    }

    @PutMapping("/users/{userId}/ban")
    @Operation(summary = "Ban a user from the platform")
    public ResponseEntity<User> banUser(@PathVariable Long userId) {
        User user = userService.banUser(userId);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/users/{userId}/unban")
    @Operation(summary = "Unban a user on the platform")
    public ResponseEntity<User> unbanUser(@PathVariable Long userId) {
        User user = userService.unbanUser(userId);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Delete a user by ID")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        ApiResponse res = new ApiResponse("User deleted successfully");
        return ResponseEntity.ok(res);
    }

    // ================= SELLER MANAGEMENT =================

    @GetMapping("/sellers")
    @Operation(summary = "Get all registered sellers")
    public ResponseEntity<List<Seller>> getAllSellers() {
        return ResponseEntity.ok(sellerService.getAllSellers());
    }

    @GetMapping("/sellers/{sellerId}")
    @Operation(summary = "Get seller details by ID")
    public ResponseEntity<Seller> getSellerById(@PathVariable Long sellerId) throws SellerException {
        return ResponseEntity.ok(sellerService.getSellerById(sellerId));
    }

    @PatchMapping("/sellers/{id}/status/{status}")
    @Operation(summary = "Update seller account status (PENDING_VERIFICATION / ACTIVE / SUSPENDED / BANNED / CLOSED)")
    public ResponseEntity<Seller> updateSellerStatus(
            @PathVariable Long id,
            @PathVariable AccountStatus status) throws SellerException {
        Seller seller = sellerService.updateSellerAccountStatus(id, status);
        return ResponseEntity.ok(seller);
    }

    @PatchMapping("/sellers/{id}/approve")
    @Operation(summary = "Approve a seller account (sets status to ACTIVE)")
    public ResponseEntity<Seller> approveSeller(@PathVariable Long id) throws SellerException {
        Seller seller = sellerService.updateSellerAccountStatus(id, AccountStatus.ACTIVE);
        return ResponseEntity.ok(seller);
    }

    @PatchMapping("/sellers/{id}/suspend")
    @Operation(summary = "Suspend a seller account")
    public ResponseEntity<Seller> suspendSeller(@PathVariable Long id) throws SellerException {
        Seller seller = sellerService.updateSellerAccountStatus(id, AccountStatus.SUSPENDED);
        return ResponseEntity.ok(seller);
    }

    @PatchMapping("/sellers/{id}/ban")
    @Operation(summary = "Ban a seller account permanently")
    public ResponseEntity<Seller> banSeller(@PathVariable Long id) throws SellerException {
        Seller seller = sellerService.updateSellerAccountStatus(id, AccountStatus.BANNED);
        return ResponseEntity.ok(seller);
    }
}