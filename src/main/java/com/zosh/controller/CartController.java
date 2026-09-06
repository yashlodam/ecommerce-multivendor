package com.zosh.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zosh.model.Cart;
import com.zosh.model.CartItem;
import com.zosh.model.Product;
import com.zosh.model.ProductVariant;
import com.zosh.model.User;
import com.zosh.repository.ProductVariantRepository;
import com.zosh.request.AddItemRequest;
import com.zosh.response.ApiResponse;
import com.zosh.service.CartItemService;
import com.zosh.service.CartService;
import com.zosh.service.ProductService;
import com.zosh.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cart")
@Tag(name = "Customer - Cart", description = "Shopping cart item management and calculation endpoints")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private CartItemService cartItemService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductVariantRepository variantRepository;

    @GetMapping
    @Operation(summary = "Get current customer's shopping cart with recalculated totals")
    public ResponseEntity<Cart> findUserCartHandler(
            @RequestHeader("Authorization") String jwt) {

        User user = userService.findUserByJwtToken(jwt);
        Cart cart = cartService.findUserCart(user);

        return ResponseEntity.ok(cart);
    }

    @PutMapping("/add")
    @Operation(summary = "Add an item to the customer's cart (supports variant selection)")
    public ResponseEntity<CartItem> addItemToCart(
            @Valid @RequestBody AddItemRequest req,
            @RequestHeader("Authorization") String jwt) {

        User user = userService.findUserByJwtToken(jwt);
        Product product = productService.findProductById(req.getProductId());

        // Resolve variant if variantId is provided
        ProductVariant variant = null;
        if (req.getVariantId() != null) {
            variant = variantRepository.findById(req.getVariantId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Variant not found with id: " + req.getVariantId()));
            // Verify variant belongs to the requested product
            if (!variant.getProduct().getId().equals(product.getId())) {
                throw new IllegalArgumentException("Variant does not belong to the specified product.");
            }
        }

        // Determine size label: use variant name if variant resolved, else fallback to req.getSize()
        String sizeLabel = variant != null ? variant.getVariantName() : req.getSize();

        CartItem item = cartService.addCartItem(user, product, variant, sizeLabel, req.getQuantity());

        return new ResponseEntity<>(item, HttpStatus.ACCEPTED);
    }

    @DeleteMapping("/item/{cartItemId}")
    @Operation(summary = "Remove an item from the cart (enforces customer ownership)")
    public ResponseEntity<ApiResponse> deleteCartItemHandler(
            @PathVariable Long cartItemId,
            @RequestHeader("Authorization") String jwt) {

        User user = userService.findUserByJwtToken(jwt);
        cartItemService.removeCartItem(user.getId(), cartItemId);

        ApiResponse res = new ApiResponse("Item removed from cart");
        return ResponseEntity.ok(res);
    }

    @PutMapping("/item/{cartItemId}")
    @Operation(summary = "Update quantity of a cart item (enforces customer ownership)")
    public ResponseEntity<CartItem> updateCartItemHandler(
            @PathVariable Long cartItemId,
            @RequestBody CartItem cartItem,
            @RequestHeader("Authorization") String jwt) {

        User user = userService.findUserByJwtToken(jwt);

        if (cartItem.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be at least 1.");
        }

        CartItem updatedCartItem = cartItemService.updateCartItem(user.getId(), cartItemId, cartItem);
        return new ResponseEntity<>(updatedCartItem, HttpStatus.ACCEPTED);
    }
}
