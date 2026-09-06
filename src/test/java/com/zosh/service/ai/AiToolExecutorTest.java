package com.zosh.service.ai;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import com.zosh.dto.chat.AiProductDto;
import com.zosh.model.Cart;
import com.zosh.model.Product;
import com.zosh.model.ProductVariant;
import com.zosh.model.Seller;
import com.zosh.model.User;
import com.zosh.service.CartItemService;
import com.zosh.service.CartService;
import com.zosh.service.OrderService;
import com.zosh.service.ProductService;
import com.zosh.service.ReviewService;
import com.zosh.service.SellerService;
import com.zosh.service.ai.tool.AiToolExecutor;

@ExtendWith(MockitoExtension.class)
class AiToolExecutorTest {

    @Mock private ProductService productService;
    @Mock private CartService cartService;
    @Mock private CartItemService cartItemService;
    @Mock private OrderService orderService;
    @Mock private SellerService sellerService;
    @Mock private ReviewService reviewService;
    @Mock private ProductRankingService rankingService;
    @Mock private StorePolicyService policyService;

    private AiToolExecutor toolExecutor;

    @BeforeEach
    void setUp() {
        toolExecutor = new AiToolExecutor(
                productService, cartService, cartItemService, orderService,
                sellerService, reviewService, rankingService, policyService);
    }

    @Test
    @DisplayName("search_products tool executes ProductService and returns ranked results")
    void testExecuteSearchProducts() {
        Product p = new Product();
        p.setId(5L);
        p.setTitle("Men Formal Shirt");

        AiProductDto dto = new AiProductDto();
        dto.setId(5L);
        dto.setTitle("Men Formal Shirt");

        when(productService.getAllProducts(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(p)));
        when(rankingService.rank(any(), any(), any())).thenReturn(List.of(dto));

        Map<String, Object> result = toolExecutor.executeTool("search_products", Map.of("query", "formal shirt"), null);

        assertNotNull(result);
        assertEquals(1, result.get("resultCount"));
        assertTrue(result.containsKey("products"));
    }

    @Test
    @DisplayName("check_variant_availability returns variant info when variant matches")
    void testCheckVariantAvailabilityMatch() {
        ProductVariant v = new ProductVariant();
        v.setId(21L);
        v.setVariantName("L");
        v.setSellingPrice(899);
        v.setQuantity(4);

        when(productService.getVariantsByProductId(5L)).thenReturn(List.of(v));

        Map<String, Object> result = toolExecutor.executeTool("check_variant_availability",
                Map.of("productId", 5, "variantName", "L"), null);

        assertNotNull(result);
        assertEquals("L", result.get("variantName"));
        assertEquals(true, result.get("inStock"));
    }

    @Test
    @DisplayName("get_cart requires authentication when user is null")
    void testGetCartUnauthenticated() {
        Map<String, Object> result = toolExecutor.executeTool("get_cart", Map.of(), null);
        assertNotNull(result);
        assertTrue(result.containsKey("requiresAuth"));
        assertEquals(true, result.get("requiresAuth"));
    }

    @Test
    @DisplayName("get_cart returns cart summary for authenticated user")
    void testGetCartAuthenticated() {
        User user = new User();
        user.setId(1L);

        Cart cart = new Cart();
        cart.setTotalItem(2);
        cart.setTotalSellingPrice(1500.0);
        cart.setCartItems(Collections.emptySet());

        when(cartService.findUserCart(user)).thenReturn(cart);

        Map<String, Object> result = toolExecutor.executeTool("get_cart", Map.of(), user);
        assertNotNull(result);
        assertTrue(result.containsKey("cart"));
    }

    @Test
    @DisplayName("get_seller_details returns safe public seller information")
    void testGetSellerDetailsSafe() throws Exception {
        Seller s = new Seller();
        s.setId(10L);
        s.setSellerName("Alpha Styles");
        s.setEmail("seller@alpha.com");
        s.setPassword("secretPassword123");

        when(sellerService.getSellerById(10L)).thenReturn(s);

        Map<String, Object> result = toolExecutor.executeTool("get_seller_details", Map.of("sellerId", 10), null);
        assertNotNull(result);
        assertTrue(result.containsKey("seller"));
        @SuppressWarnings("unchecked")
        Map<String, Object> sellerMap = (Map<String, Object>) result.get("seller");
        assertEquals("Alpha Styles", sellerMap.get("sellerName"));
        assertFalse(sellerMap.containsKey("password"), "Password must never be exposed");
    }

    @Test
    @DisplayName("get_store_policy returns policy text from StorePolicyService")
    void testGetStorePolicy() {
        when(policyService.getReturnPolicy()).thenReturn("7-day return policy");

        Map<String, Object> result = toolExecutor.executeTool("get_store_policy",
                Map.of("policyType", "returns"), null);

        assertNotNull(result);
        assertEquals("7-day return policy", result.get("policy"));
    }
}
