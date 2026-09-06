package com.zosh.service.ai.tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zosh.dto.chat.AiCartSummaryDto;
import com.zosh.dto.chat.AiOrderSummaryDto;
import com.zosh.dto.chat.AiProductDto;
import com.zosh.model.Cart;
import com.zosh.model.CartItem;
import com.zosh.model.Order;
import com.zosh.model.Product;
import com.zosh.model.ProductVariant;
import com.zosh.model.Review;
import com.zosh.model.Seller;
import com.zosh.model.User;
import com.zosh.service.CartItemService;
import com.zosh.service.CartService;
import com.zosh.service.OrderService;
import com.zosh.service.ProductService;
import com.zosh.service.ReviewService;
import com.zosh.service.SellerService;
import com.zosh.service.ai.ProductRankingService;
import com.zosh.service.ai.StorePolicyService;

@Service
public class AiToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(AiToolExecutor.class);

    private final ProductService productService;
    private final CartService cartService;
    private final CartItemService cartItemService;
    private final OrderService orderService;
    private final SellerService sellerService;
    private final ReviewService reviewService;
    private final ProductRankingService rankingService;
    private final StorePolicyService policyService;

    public AiToolExecutor(ProductService productService,
                          CartService cartService,
                          CartItemService cartItemService,
                          OrderService orderService,
                          SellerService sellerService,
                          ReviewService reviewService,
                          ProductRankingService rankingService,
                          StorePolicyService policyService) {
        this.productService = productService;
        this.cartService = cartService;
        this.cartItemService = cartItemService;
        this.orderService = orderService;
        this.sellerService = sellerService;
        this.reviewService = reviewService;
        this.rankingService = rankingService;
        this.policyService = policyService;
    }

    // ─── Tool Declarations for Gemini Function Calling ────────────────────

    public List<Map<String, Object>> getToolDeclarations() {
        List<Map<String, Object>> tools = new ArrayList<>();

        // 1. search_products
        tools.add(buildToolDeclaration("search_products",
                "Search real products in the ShopSphere catalog by keyword, category, brand, price range, color, or size.",
                Map.of(
                        "query", Map.of("type", "STRING", "description", "Search keywords (e.g. 'formal shirt', 'black shirt', 'gaming phone')"),
                        "category", Map.of("type", "STRING", "description", "Category ID (e.g. 'men_formal_shirts'). Only provide if known; otherwise omit."),
                        "brand", Map.of("type", "STRING", "description", "Brand filter"),
                        "minPrice", Map.of("type", "INTEGER", "description", "Minimum price in INR"),
                        "maxPrice", Map.of("type", "INTEGER", "description", "Maximum price in INR"),
                        "color", Map.of("type", "STRING", "description", "Color filter (e.g. 'Teal', 'Black')"),
                        "size", Map.of("type", "STRING", "description", "Size filter (e.g. 'S', 'M', 'L', 'XL')")
                ),
                List.of()
        ));

        // 2. get_product_details
        tools.add(buildToolDeclaration("get_product_details",
                "Get full details for a product including all real variants, prices, and stock.",
                Map.of("productId", Map.of("type", "INTEGER", "description", "Product ID to inspect")),
                List.of("productId")
        ));

        // 3. check_variant_availability
        tools.add(buildToolDeclaration("check_variant_availability",
                "Check whether a specific variant/size is in stock for a product.",
                Map.of(
                        "productId", Map.of("type", "INTEGER", "description", "Product ID"),
                        "variantName", Map.of("type", "STRING", "description", "Variant name to check (e.g. 'S', 'M', 'L', 'XL')")
                ),
                List.of("productId", "variantName")
        ));

        // 4. compare_products
        tools.add(buildToolDeclaration("compare_products",
                "Compare 2 or 3 products side-by-side using real database attributes.",
                Map.of(
                        "productId1", Map.of("type", "INTEGER", "description", "First product ID"),
                        "productId2", Map.of("type", "INTEGER", "description", "Second product ID"),
                        "productId3", Map.of("type", "INTEGER", "description", "Optional third product ID")
                ),
                List.of("productId1", "productId2")
        ));

        // 5. find_similar_products
        tools.add(buildToolDeclaration("find_similar_products",
                "Find other products in the same category as a reference product.",
                Map.of("productId", Map.of("type", "INTEGER", "description", "Reference product ID")),
                List.of("productId")
        ));

        // 6. get_cart
        tools.add(buildToolDeclaration("get_cart",
                "Get current shopping cart contents for the authenticated user.",
                Map.of(),
                List.of()
        ));

        // 7. add_to_cart
        tools.add(buildToolDeclaration("add_to_cart",
                "Add a product (and variant) to the authenticated user's cart.",
                Map.of(
                        "productId", Map.of("type", "INTEGER", "description", "Product ID"),
                        "variantName", Map.of("type", "STRING", "description", "Variant size (e.g. 'M', 'L')"),
                        "quantity", Map.of("type", "INTEGER", "description", "Quantity to add (default 1)")
                ),
                List.of("productId")
        ));

        // 8. update_cart_quantity
        tools.add(buildToolDeclaration("update_cart_quantity",
                "Update quantity of an item already in the cart.",
                Map.of(
                        "cartItemId", Map.of("type", "INTEGER", "description", "Cart item ID"),
                        "quantity", Map.of("type", "INTEGER", "description", "New quantity")
                ),
                List.of("cartItemId", "quantity")
        ));

        // 9. remove_from_cart
        tools.add(buildToolDeclaration("remove_from_cart",
                "Remove an item from the cart.",
                Map.of("cartItemId", Map.of("type", "INTEGER", "description", "Cart item ID to remove")),
                List.of("cartItemId")
        ));

        // 10. clear_cart
        tools.add(buildToolDeclaration("clear_cart",
                "Remove all items from the user's cart.",
                Map.of(),
                List.of()
        ));

        // 11. get_my_orders
        tools.add(buildToolDeclaration("get_my_orders",
                "Get the authenticated user's order history.",
                Map.of(),
                List.of()
        ));

        // 12. get_latest_order
        tools.add(buildToolDeclaration("get_latest_order",
                "Get the most recent order placed by the authenticated user.",
                Map.of(),
                List.of()
        ));

        // 13. cancel_order
        tools.add(buildToolDeclaration("cancel_order",
                "Cancel an unshipped order belonging to the authenticated user.",
                Map.of("orderId", Map.of("type", "INTEGER", "description", "Order ID to cancel")),
                List.of("orderId")
        ));

        // 14. get_seller_details
        tools.add(buildToolDeclaration("get_seller_details",
                "Get public verified seller information (name, business name, verification status).",
                Map.of("sellerId", Map.of("type", "INTEGER", "description", "Seller ID")),
                List.of("sellerId")
        ));

        // 15. get_product_reviews
        tools.add(buildToolDeclaration("get_product_reviews",
                "Get customer reviews and ratings for a product.",
                Map.of("productId", Map.of("type", "INTEGER", "description", "Product ID")),
                List.of("productId")
        ));

        // 16. get_store_policy
        tools.add(buildToolDeclaration("get_store_policy",
                "Get verified store policies: returns, shipping, payment methods, or FAQs.",
                Map.of("policyType", Map.of("type", "STRING", "description", "Policy type: 'returns', 'shipping', 'payment', 'authenticity', or 'faq'")),
                List.of("policyType")
        ));

        return tools;
    }

    // ─── Execution Router ─────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> executeTool(String toolName, Map<String, Object> args, User user) {
        log.info("Executing AI tool: {} with args: {}", toolName, args);
        try {
            return switch (toolName) {
                case "search_products" -> executeSearchProducts(args);
                case "get_product_details" -> executeGetProductDetails(args);
                case "check_variant_availability" -> executeCheckVariantAvailability(args);
                case "compare_products" -> executeCompareProducts(args);
                case "find_similar_products" -> executeFindSimilarProducts(args);
                case "get_cart" -> executeGetCart(user);
                case "add_to_cart" -> executeAddToCart(args, user);
                case "update_cart_quantity" -> executeUpdateCartQuantity(args, user);
                case "remove_from_cart" -> executeRemoveFromCart(args, user);
                case "clear_cart" -> executeClearCart(user);
                case "get_my_orders" -> executeGetMyOrders(user);
                case "get_latest_order" -> executeGetLatestOrder(user);
                case "cancel_order" -> executeCancelOrder(args, user);
                case "get_seller_details" -> executeGetSellerDetails(args);
                case "get_product_reviews" -> executeGetProductReviews(args);
                case "get_store_policy" -> executeGetStorePolicy(args);
                default -> Map.of("error", "Unknown tool: " + toolName);
            };
        } catch (Exception e) {
            log.error("Tool execution failed for {}: {}", toolName, e.getMessage(), e);
            return Map.of("error", "Tool execution failed: " + e.getMessage());
        }
    }

    // ─── Individual Tool Implementations ──────────────────────────────────

    private Map<String, Object> executeSearchProducts(Map<String, Object> args) {
        String query = getStringArg(args, "query", "");
        String category = getStringArg(args, "category", null);
        String brand = getStringArg(args, "brand", null);
        Integer minPrice = getIntArg(args, "minPrice");
        Integer maxPrice = getIntArg(args, "maxPrice");
        String color = getStringArg(args, "color", null);
        String size = getStringArg(args, "size", null);

        String cleanQuery = cleanSearchQuery(query);

        Page<Product> page = productService.getAllProducts(
                cleanQuery, category, brand, color, size,
                minPrice, maxPrice, null, "price_low", "in_stock", 0);

        // Fallback 1: If category filter yielded 0 results, retry without the category filter
        if (page.isEmpty() && category != null) {
            page = productService.getAllProducts(
                    cleanQuery, null, brand, color, size,
                    minPrice, maxPrice, null, "price_low", "in_stock", 0);
        }

        // Fallback 2: If query ends with 's' (plural) and yielded 0 results, try singular form
        if (page.isEmpty() && cleanQuery != null && cleanQuery.length() > 3 && cleanQuery.endsWith("s")) {
            String singular = cleanQuery.substring(0, cleanQuery.length() - 1);
            page = productService.getAllProducts(
                    singular, null, brand, color, size,
                    minPrice, maxPrice, null, "price_low", "in_stock", 0);
        }

        // Fallback 3: If query is singular and yielded 0 results, try plural form
        if (page.isEmpty() && cleanQuery != null && !cleanQuery.isBlank() && !cleanQuery.endsWith("s")) {
            String plural = cleanQuery + "s";
            page = productService.getAllProducts(
                    plural, null, brand, color, size,
                    minPrice, maxPrice, null, "price_low", "in_stock", 0);
        }

        List<AiProductDto> ranked = rankingService.rank(page.getContent(), maxPrice, query);

        Map<String, Object> result = new HashMap<>();
        result.put("totalResults", page.getTotalElements());
        result.put("products", ranked);
        result.put("resultCount", ranked.size());
        return result;
    }

    private String cleanSearchQuery(String query) {
        if (query == null || query.isBlank()) return "";
        String cleaned = query.replaceAll("(?i)\\b(show|me|find|get|give|look|looking|for|a|an|the|best|good|can|you|please|some|any|all|i|want|need|buy|recommend|top|product|products|item|items)\\b", " ");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        return cleaned;
    }

    private Map<String, Object> executeGetProductDetails(Map<String, Object> args) {
        Long productId = getLongArg(args, "productId");
        if (productId == null) return Map.of("error", "productId is required");

        Product product = productService.findProductById(productId);
        AiProductDto dto = rankingService.toDto(product);

        Map<String, Object> result = new HashMap<>();
        result.put("product", dto);
        return result;
    }

    private Map<String, Object> executeCheckVariantAvailability(Map<String, Object> args) {
        Long productId = getLongArg(args, "productId");
        String variantName = getStringArg(args, "variantName", "");
        if (productId == null) return Map.of("error", "productId is required");

        List<ProductVariant> variants = productService.getVariantsByProductId(productId);
        String lowerVariant = variantName.toLowerCase().trim();

        for (ProductVariant v : variants) {
            if (v.getVariantName() != null && v.getVariantName().toLowerCase().trim().equals(lowerVariant)) {
                Map<String, Object> result = new HashMap<>();
                result.put("variantId", v.getId());
                result.put("variantName", v.getVariantName());
                result.put("sellingPrice", v.getSellingPrice());
                result.put("mrpPrice", v.getMrpPrice());
                result.put("quantity", v.getQuantity());
                result.put("inStock", v.getQuantity() != null && v.getQuantity() > 0);
                return result;
            }
        }

        List<String> available = variants.stream()
                .map(ProductVariant::getVariantName)
                .collect(Collectors.toList());
        return Map.of("error", "Variant '" + variantName + "' not found", "availableVariants", available);
    }

    private Map<String, Object> executeCompareProducts(Map<String, Object> args) {
        Long id1 = getLongArg(args, "productId1");
        Long id2 = getLongArg(args, "productId2");
        Long id3 = getLongArg(args, "productId3");

        List<AiProductDto> compared = new ArrayList<>();
        if (id1 != null) {
            try { compared.add(rankingService.toDto(productService.findProductById(id1))); } catch (Exception ignored) {}
        }
        if (id2 != null) {
            try { compared.add(rankingService.toDto(productService.findProductById(id2))); } catch (Exception ignored) {}
        }
        if (id3 != null) {
            try { compared.add(rankingService.toDto(productService.findProductById(id3))); } catch (Exception ignored) {}
        }

        Map<String, Object> result = new HashMap<>();
        result.put("products", compared);
        result.put("comparisonCount", compared.size());
        return result;
    }

    private Map<String, Object> executeFindSimilarProducts(Map<String, Object> args) {
        Long productId = getLongArg(args, "productId");
        if (productId == null) return Map.of("error", "productId is required");

        Product reference = productService.findProductById(productId);
        String categoryId = reference.getCategory() != null ? reference.getCategory().getCategoryId() : null;

        Page<Product> page = productService.getAllProducts(
                null, categoryId, null, null, null,
                null, null, null, "price_low", "in_stock", 0);

        List<AiProductDto> similar = page.getContent().stream()
                .filter(p -> !p.getId().equals(productId))
                .limit(4)
                .map(rankingService::toDto)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("products", similar);
        result.put("resultCount", similar.size());
        return result;
    }

    private Map<String, Object> executeGetCart(User user) {
        if (user == null) {
            return Map.of("error", "Authentication required. Please sign in to view your cart.", "requiresAuth", true);
        }

        Cart cart = cartService.findUserCart(user);
        AiCartSummaryDto summary = buildCartSummary(cart);

        Map<String, Object> result = new HashMap<>();
        result.put("cart", summary);
        return result;
    }

    private Map<String, Object> executeAddToCart(Map<String, Object> args, User user) {
        if (user == null) {
            return Map.of("error", "Authentication required. Please sign in to add items to your cart.", "requiresAuth", true);
        }

        Long productId = getLongArg(args, "productId");
        if (productId == null) return Map.of("error", "productId is required");

        String variantName = getStringArg(args, "variantName", null);
        int quantity = getIntArg(args, "quantity") != null ? getIntArg(args, "quantity") : 1;

        Product product = productService.findProductById(productId);
        ProductVariant variant = null;
        String size = variantName;

        if (variantName != null && !variantName.isBlank()) {
            List<ProductVariant> variants = productService.getVariantsByProductId(productId);
            String lower = variantName.toLowerCase().trim();
            for (ProductVariant v : variants) {
                if (v.getVariantName() != null && v.getVariantName().toLowerCase().trim().equals(lower)) {
                    variant = v;
                    size = v.getVariantName();
                    break;
                }
            }
            if (variant == null) {
                List<String> available = variants.stream().map(ProductVariant::getVariantName).collect(Collectors.toList());
                return Map.of("error", "Variant '" + variantName + "' not found for this product", "availableVariants", available);
            }
            if (variant.getQuantity() != null && variant.getQuantity() < quantity) {
                return Map.of("error", "Insufficient stock for size " + variantName + ". Only " + variant.getQuantity() + " available.");
            }
        }

        if (variant != null) {
            cartService.addCartItem(user, product, variant, size, quantity);
        } else {
            cartService.addCartItem(user, product, size, quantity);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Added " + product.getTitle() + (variant != null ? " (size " + variant.getVariantName() + ")" : "") + " to your cart.");
        result.put("productId", productId);
        if (variant != null) result.put("variantId", variant.getId());
        return result;
    }

    private Map<String, Object> executeUpdateCartQuantity(Map<String, Object> args, User user) {
        if (user == null) return Map.of("error", "Authentication required.", "requiresAuth", true);
        Long cartItemId = getLongArg(args, "cartItemId");
        Integer quantity = getIntArg(args, "quantity");
        if (cartItemId == null || quantity == null || quantity <= 0) {
            return Map.of("error", "Valid cartItemId and quantity > 0 are required.");
        }

        CartItem item = new CartItem();
        item.setQuantity(quantity);
        CartItem updated = cartItemService.updateCartItem(user.getId(), cartItemId, item);
        return Map.of("success", true, "message", "Cart quantity updated to " + updated.getQuantity());
    }

    private Map<String, Object> executeRemoveFromCart(Map<String, Object> args, User user) {
        if (user == null) return Map.of("error", "Authentication required.", "requiresAuth", true);
        Long cartItemId = getLongArg(args, "cartItemId");
        if (cartItemId == null) return Map.of("error", "cartItemId is required.");

        cartItemService.removeCartItem(user.getId(), cartItemId);
        return Map.of("success", true, "message", "Item removed from your cart.");
    }

    private Map<String, Object> executeClearCart(User user) {
        if (user == null) return Map.of("error", "Authentication required.", "requiresAuth", true);
        cartService.clearCart(user);
        return Map.of("success", true, "message", "Your cart has been emptied.");
    }

    private Map<String, Object> executeGetMyOrders(User user) {
        if (user == null) {
            return Map.of("error", "Authentication required. Please sign in to view your orders.", "requiresAuth", true);
        }

        List<Order> orders = orderService.usersOrderHistory(user.getId());
        List<AiOrderSummaryDto> summaries = orders.stream().map(this::buildOrderSummary).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("orders", summaries);
        result.put("totalOrders", summaries.size());
        return result;
    }

    private Map<String, Object> executeGetLatestOrder(User user) {
        if (user == null) {
            return Map.of("error", "Authentication required. Please sign in to view your latest order.", "requiresAuth", true);
        }

        List<Order> orders = orderService.usersOrderHistory(user.getId());
        if (orders.isEmpty()) {
            return Map.of("message", "You don't have any past orders.");
        }

        AiOrderSummaryDto latest = buildOrderSummary(orders.get(0));
        return Map.of("latestOrder", latest);
    }

    private Map<String, Object> executeCancelOrder(Map<String, Object> args, User user) {
        if (user == null) {
            return Map.of("error", "Authentication required to cancel orders.", "requiresAuth", true);
        }

        Long orderId = getLongArg(args, "orderId");
        if (orderId == null) return Map.of("error", "orderId is required");

        try {
            Order cancelled = orderService.cancelOrder(orderId, user);
            return Map.of(
                    "success", true,
                    "message", "Order #" + orderId + " has been cancelled successfully.",
                    "orderId", orderId,
                    "newStatus", cancelled.getOrderStatus().name()
            );
        } catch (Exception e) {
            return Map.of("error", "Could not cancel order: " + e.getMessage());
        }
    }

    private Map<String, Object> executeGetSellerDetails(Map<String, Object> args) {
        Long sellerId = getLongArg(args, "sellerId");
        if (sellerId == null) return Map.of("error", "sellerId is required");

        try {
            Seller seller = sellerService.getSellerById(sellerId);
            // Return safe public fields only — never passwords, bank accounts, or GSTIN
            Map<String, Object> publicInfo = new HashMap<>();
            publicInfo.put("sellerId", seller.getId());
            publicInfo.put("sellerName", seller.getSellerName());
            publicInfo.put("businessName", seller.getBusinesssDetails() != null ? seller.getBusinesssDetails().getBusinessName() : seller.getSellerName());
            publicInfo.put("isEmailVerified", seller.isEmailVerified());
            publicInfo.put("accountStatus", seller.getAccountStatus() != null ? seller.getAccountStatus().name() : "ACTIVE");
            return Map.of("seller", publicInfo);
        } catch (Exception e) {
            return Map.of("error", "Seller not found: " + e.getMessage());
        }
    }

    private Map<String, Object> executeGetProductReviews(Map<String, Object> args) {
        Long productId = getLongArg(args, "productId");
        if (productId == null) return Map.of("error", "productId is required");

        List<Review> reviews = reviewService.getReviewByProductId(productId);
        List<Map<String, Object>> safeReviews = reviews.stream().limit(5).map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("rating", r.getRating());
            map.put("reviewText", r.getReviewText());
            map.put("reviewerName", r.getUser() != null ? r.getUser().getFullName() : "Customer");
            return map;
        }).collect(Collectors.toList());

        double avgRating = reviews.stream().mapToDouble(Review::getRating).average().orElse(0.0);
        return Map.of(
                "totalReviews", reviews.size(),
                "averageRating", Math.round(avgRating * 10.0) / 10.0,
                "recentReviews", safeReviews
        );
    }

    private Map<String, Object> executeGetStorePolicy(Map<String, Object> args) {
        String policyType = getStringArg(args, "policyType", "faq").toLowerCase();

        String policy = switch (policyType) {
            case "returns", "return", "refund" -> policyService.getReturnPolicy();
            case "shipping", "delivery" -> policyService.getShippingPolicy();
            case "payment", "payments", "cod" -> policyService.getPaymentMethods();
            case "authenticity", "genuine", "authentic" -> policyService.getAuthenticityGuarantee();
            default -> policyService.getFAQ();
        };

        return Map.of("policy", policy, "policyType", policyType);
    }

    // ─── Helper Builders ──────────────────────────────────────────────────

    private AiCartSummaryDto buildCartSummary(Cart cart) {
        AiCartSummaryDto summary = new AiCartSummaryDto();
        summary.setTotalItem(cart.getTotalItem());
        summary.setTotalSellingPrice(cart.getTotalSellingPrice());
        summary.setTotalMrpPrice(cart.getTotalMrpPrice());
        summary.setDiscount(cart.getDiscount());

        List<AiCartSummaryDto.AiCartItemDto> items = new ArrayList<>();
        if (cart.getCartItems() != null) {
            for (CartItem ci : cart.getCartItems()) {
                items.add(new AiCartSummaryDto.AiCartItemDto(
                        ci.getId(),
                        ci.getProduct() != null ? ci.getProduct().getId() : null,
                        ci.getVariant() != null ? ci.getVariant().getId() : null,
                        ci.getProduct() != null ? ci.getProduct().getTitle() : "Unknown",
                        ci.getVariant() != null ? ci.getVariant().getVariantName() : ci.getSize(),
                        ci.getQuantity(),
                        ci.getSellingPrice()
                ));
            }
        }
        summary.setItems(items);
        return summary;
    }

    private AiOrderSummaryDto buildOrderSummary(Order order) {
        AiOrderSummaryDto dto = new AiOrderSummaryDto();
        dto.setOrderId(order.getId());
        dto.setOrderStatus(order.getOrderStatus() != null ? order.getOrderStatus().name() : "UNKNOWN");
        dto.setTotalAmount(order.getTotalMrpPrice());
        dto.setOrderDate(order.getOrderDate());
        dto.setDeliveryDate(order.getDeliverDate());
        if (order.getShippingAddress() != null) {
            dto.setShippingAddress(order.getShippingAddress().getAddress());
        }
        if (order.getOrderItems() != null) {
            dto.setItemCount(order.getOrderItems().size());
            List<String> titles = order.getOrderItems().stream()
                    .filter(oi -> oi.getProduct() != null)
                    .map(oi -> oi.getProduct().getTitle())
                    .collect(Collectors.toList());
            dto.setItemTitles(titles);
        }
        return dto;
    }

    private Map<String, Object> buildToolDeclaration(String name, String description,
                                                      Map<String, Map<String, String>> properties,
                                                      List<String> required) {
        Map<String, Object> decl = new LinkedHashMap<>();
        decl.put("name", name);
        decl.put("description", description);

        if (!properties.isEmpty()) {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("type", "OBJECT");
            params.put("properties", properties);
            if (required != null && !required.isEmpty()) {
                params.put("required", required);
            }
            decl.put("parameters", params);
        }
        return decl;
    }

    private String getStringArg(Map<String, Object> args, String key, String defaultVal) {
        if (args == null || !args.containsKey(key)) return defaultVal;
        Object val = args.get(key);
        return val != null ? val.toString() : defaultVal;
    }

    private Integer getIntArg(Map<String, Object> args, String key) {
        if (args == null || !args.containsKey(key)) return null;
        Object val = args.get(key);
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).intValue();
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long getLongArg(Map<String, Object> args, String key) {
        if (args == null || !args.containsKey(key)) return null;
        Object val = args.get(key);
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
