package com.zosh.service.ai;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zosh.dto.chat.AiProductDto;
import com.zosh.model.Category;
import com.zosh.model.Product;
import com.zosh.model.ProductVariant;

class ProductRankingServiceTest {

    private ProductRankingService rankingService;

    @BeforeEach
    void setUp() {
        rankingService = new ProductRankingService();
    }

    @Test
    @DisplayName("toDto properly maps Product entity and its variants")
    void testToDto() {
        Product p = new Product();
        p.setId(10L);
        p.setTitle("Casual Navy Shirt");
        p.setBrand("Allen Solly");
        p.setColor("Navy");
        p.setMrpPrice(1500);
        p.setSellingPrice(999);
        p.setDiscountPercent(33);
        p.setQuantity(15);
        p.setNumRatings(25);

        Category cat = new Category();
        cat.setName("Men Shirts");
        p.setCategory(cat);

        ProductVariant v1 = new ProductVariant();
        v1.setId(101L);
        v1.setVariantName("M");
        v1.setMrpPrice(1500);
        v1.setSellingPrice(999);
        v1.setQuantity(5);
        v1.setDefault(true);

        p.setVariants(List.of(v1));

        AiProductDto dto = rankingService.toDto(p);

        assertNotNull(dto);
        assertEquals(10L, dto.getId());
        assertEquals("Casual Navy Shirt", dto.getTitle());
        assertEquals("Allen Solly", dto.getBrand());
        assertEquals("Navy", dto.getColor());
        assertEquals(999, dto.getSellingPrice());
        assertTrue(dto.isInStock());
        assertEquals(1, dto.getVariants().size());
        assertEquals("M", dto.getVariants().get(0).getVariantName());
        assertTrue(dto.getVariants().get(0).isInStock());
    }

    @Test
    @DisplayName("rank boosts in-stock and under-budget products")
    void testRankBudgetAndStock() {
        Product pCheapInStock = new Product();
        pCheapInStock.setId(1L);
        pCheapInStock.setTitle("Formal Shirt Budget");
        pCheapInStock.setSellingPrice(800);
        pCheapInStock.setQuantity(10);

        Product pExpensive = new Product();
        pExpensive.setId(2L);
        pExpensive.setTitle("Formal Shirt Expensive");
        pExpensive.setSellingPrice(2500);
        pExpensive.setQuantity(10);

        Product pOutOfStock = new Product();
        pOutOfStock.setId(3L);
        pOutOfStock.setTitle("Formal Shirt OutOfStock");
        pOutOfStock.setSellingPrice(750);
        pOutOfStock.setQuantity(0);

        List<Product> products = new ArrayList<>(List.of(pExpensive, pOutOfStock, pCheapInStock));

        List<AiProductDto> ranked = rankingService.rank(products, 1000, "Formal");

        assertNotNull(ranked);
        assertEquals(3, ranked.size());
        assertEquals(1L, ranked.get(0).getId(), "In-stock under-budget item should be ranked first");
    }
}
