package com.zosh.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.zosh.domain.AccountStatus;
import com.zosh.exceptions.SellerException;
import com.zosh.model.Seller;
import com.zosh.model.User;
import com.zosh.service.SellerService;
import com.zosh.service.UserService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private SellerService sellerService;

    @Autowired
    private UserService userService;

    // ================= USER MANAGEMENT =================

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {

        return ResponseEntity.ok(
                userService.getAllUsers()
        );
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<User> getUserById(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                userService.findUserById(userId)
        );
    }

    @PutMapping("/users/{userId}/ban")
    public ResponseEntity<User> banUser(
            @PathVariable Long userId) {

        User user = userService.banUser(userId);

        return ResponseEntity.ok(user);
    }

    @PutMapping("/users/{userId}/unban")
    public ResponseEntity<User> unbanUser(
            @PathVariable Long userId) {

        User user = userService.unbanUser(userId);

        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<String> deleteUser(
            @PathVariable Long userId) {

        userService.deleteUser(userId);

        return ResponseEntity.ok("User Deleted Successfully");
    }

    // ================= SELLER MANAGEMENT =================

    @GetMapping("/sellers")
    public ResponseEntity<List<Seller>> getAllSellers() {

        return ResponseEntity.ok(
                sellerService.getAllSellers()
        );
    }

    @GetMapping("/sellers/{sellerId}")
    public ResponseEntity<Seller> getSellerById(
            @PathVariable Long sellerId)
            throws SellerException {

        return ResponseEntity.ok(
                sellerService.getSellerById(sellerId)
        );
    }

    @PatchMapping("/sellers/{id}/status/{status}")
    public ResponseEntity<Seller> updateSellerStatus(
            @PathVariable Long id,
            @PathVariable AccountStatus status)
            throws SellerException {

        Seller seller =
                sellerService.updateSellerAccountStatus(
                        id,
                        status
                );

        return ResponseEntity.ok(seller);
    }

    @PatchMapping("/sellers/{id}/approve")
    public ResponseEntity<Seller> approveSeller(
            @PathVariable Long id)
            throws SellerException {

        Seller seller =
                sellerService.updateSellerAccountStatus(
                        id,
                        AccountStatus.ACTIVE
                );

        return ResponseEntity.ok(seller);
    }

    @PatchMapping("/sellers/{id}/suspend")
    public ResponseEntity<Seller> suspendSeller(
            @PathVariable Long id)
            throws SellerException {

        Seller seller =
                sellerService.updateSellerAccountStatus(
                        id,
                        AccountStatus.SUSPENDED
                );

        return ResponseEntity.ok(seller);
    }

    @PatchMapping("/sellers/{id}/ban")
    public ResponseEntity<Seller> banSeller(
            @PathVariable Long id)
            throws SellerException {

        Seller seller =
                sellerService.updateSellerAccountStatus(
                        id,
                        AccountStatus.BANNED
                );

        return ResponseEntity.ok(seller);
    }
}