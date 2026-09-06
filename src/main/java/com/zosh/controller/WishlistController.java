package com.zosh.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zosh.model.Product;
import com.zosh.model.User;
import com.zosh.model.Wishlist;
import com.zosh.service.ProductService;
import com.zosh.service.UserService;
import com.zosh.service.WishlistService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/wishlist")
@Tag(name = "Customer - Wishlist", description = "Customer wishlist management endpoints")
public class WishlistController {

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    @GetMapping
    @Operation(summary = "Get current customer's wishlist")
    public ResponseEntity<Wishlist> getWishlistByUserId(
            @RequestHeader("Authorization") String jwt) {
        User user = userService.findUserByJwtToken(jwt);
        Wishlist w = wishlistService.getWishlistByUserId(user);
        return ResponseEntity.ok(w);
    }

    @PostMapping("/add-product/{productId}")
    @Operation(summary = "Add or remove a product from customer wishlist (toggle)")
    public ResponseEntity<Wishlist> addProductToWishlist(
            @PathVariable Long productId,
            @RequestHeader("Authorization") String jwt) {
        Product product = productService.findProductById(productId);
        User user = userService.findUserByJwtToken(jwt);
        Wishlist w = wishlistService.addProductToWishlist(user, product);
        return new ResponseEntity<>(w, HttpStatus.OK);
    }

    @DeleteMapping("/product/{productId}")
    @Operation(summary = "Remove a product from customer wishlist")
    public ResponseEntity<Wishlist> removeProductFromWishlist(
            @PathVariable Long productId,
            @RequestHeader("Authorization") String jwt) {
        User user = userService.findUserByJwtToken(jwt);
        Wishlist w = wishlistService.removeProductFromWishlist(user, productId);
        return ResponseEntity.ok(w);
    }

    @DeleteMapping("/remove-product/{productId}")
    @Operation(summary = "Remove a product from customer wishlist (alias)")
    public ResponseEntity<Wishlist> removeProductFromWishlistAlias(
            @PathVariable Long productId,
            @RequestHeader("Authorization") String jwt) {
        User user = userService.findUserByJwtToken(jwt);
        Wishlist w = wishlistService.removeProductFromWishlist(user, productId);
        return ResponseEntity.ok(w);
    }
}
