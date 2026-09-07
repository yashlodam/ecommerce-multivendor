package com.zosh.service.search;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zosh.model.Category;
import com.zosh.model.Product;
import com.zosh.repository.CategoryRepository;
import com.zosh.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
public class ProductSearchServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private CatalogDictionaryService dictionaryService;
    private SearchQueryParser queryParser;
    private SearchRelevanceRanker relevanceRanker;

    @BeforeEach
    void setUp() {
        dictionaryService = new CatalogDictionaryService(productRepository, categoryRepository);
        queryParser = new SearchQueryParser(dictionaryService);
        relevanceRanker = new SearchRelevanceRanker();
    }

    // ─── 1. Query Parser & Normalization Tests ─────────────────────────────

    @Test
    @DisplayName("QueryParser — normalizes case, collapses spaces, and strips special characters")
    void testQueryParser_Normalization() {
        SearchQueryParser.ParsedSearchQuery result = queryParser.parse("   iPHONE    16   !!!  ");
        assertEquals("iphone 16", result.getCleanQuery());
        assertEquals(2, result.getTokens().size());
        assertEquals("iphone", result.getTokens().get(0));
        assertEquals("16", result.getTokens().get(1));
    }

    @Test
    @DisplayName("QueryParser — filters common stop-words when meaningful tokens exist")
    void testQueryParser_StopWords() {
        SearchQueryParser.ParsedSearchQuery result = queryParser.parse("blue shirt for men with discount");
        assertEquals(4, result.getTokens().size());
        assertTrue(result.getTokens().contains("blue"));
        assertTrue(result.getTokens().contains("shirt"));
        assertTrue(result.getTokens().contains("men"));
        assertTrue(result.getTokens().contains("discount"));
        assertFalse(result.getTokens().contains("for"));
        assertFalse(result.getTokens().contains("with"));
    }

    @Test
    @DisplayName("QueryParser — extracts natural price intent 'under 1500'")
    void testQueryParser_NaturalPrice_Under() {
        SearchQueryParser.ParsedSearchQuery result = queryParser.parse("formal shirts under 1500");
        assertEquals(1500, result.getExtractedMaxPrice());
        assertNull(result.getExtractedMinPrice());
        assertTrue(result.getTokens().contains("formal"));
        assertTrue(result.getTokens().contains("shirts") || result.getTokens().contains("shirt"));
        assertFalse(result.getTokens().contains("under"));
        assertFalse(result.getTokens().contains("1500"));
    }

    @Test
    @DisplayName("QueryParser — extracts natural price intent 'between 20000 and 40000'")
    void testQueryParser_NaturalPrice_Between() {
        SearchQueryParser.ParsedSearchQuery result = queryParser.parse("phones between 20000 and 40000");
        assertEquals(20000, result.getExtractedMinPrice());
        assertEquals(40000, result.getExtractedMaxPrice());
        assertTrue(result.getTokens().contains("phones") || result.getTokens().contains("phone"));
    }

    // ─── 2. Typo Tolerance & Spelling Tests ───────────────────────────────

    @Test
    @DisplayName("CatalogDictionary — recovers common typos for brands and categories")
    void testCatalogDictionary_TypoRecovery() {
        assertEquals("iphone", dictionaryService.findClosestTerm("ipone").orElse(""));
        assertEquals("iphone", dictionaryService.findClosestTerm("iphon").orElse(""));
        assertEquals("samsung", dictionaryService.findClosestTerm("smasung").orElse(""));
        assertEquals("oneplus", dictionaryService.findClosestTerm("onepls").orElse(""));
        assertEquals("laptop", dictionaryService.findClosestTerm("lapotp").orElse(""));
        assertEquals("shirt", dictionaryService.findClosestTerm("shrit").orElse(""));
        assertEquals("kurta", dictionaryService.findClosestTerm("kurti").orElse(""));
    }

    @Test
    @DisplayName("QueryParser — marks query as corrected when typo is encountered")
    void testQueryParser_CorrectedQuery() {
        SearchQueryParser.ParsedSearchQuery result = queryParser.parse("lapotp gaming");
        assertTrue(result.isCorrected());
        assertEquals("laptop gaming", result.getCorrectedQuery());
    }

    // ─── 3. Relevance Ranking Strategy Tests ──────────────────────────────

    @Test
    @DisplayName("RelevanceRanker — prioritizes exact title match 'iPhone 16' over longer variations")
    void testRelevanceRanker_ExactTitleMatchesFirst() {
        Product exactMatch = new Product();
        exactMatch.setId(8L);
        exactMatch.setTitle("iPhone 16");
        exactMatch.setBrand("Apple");
        exactMatch.setQuantity(10);
        exactMatch.setCreatedAt(LocalDateTime.now());

        Product longerMatch = new Product();
        longerMatch.setId(14L);
        longerMatch.setTitle("Apple iPhone 16 Pro Max 5G Smartphone");
        longerMatch.setBrand("Apple");
        longerMatch.setQuantity(5);
        longerMatch.setCreatedAt(LocalDateTime.now());

        SearchQueryParser.ParsedSearchQuery query = queryParser.parse("iphone 16");
        List<Product> ranked = relevanceRanker.rank(List.of(longerMatch, exactMatch), query);

        assertEquals(2, ranked.size());
        assertEquals(exactMatch.getId(), ranked.get(0).getId(), "Exact iPhone 16 should rank first");
        assertEquals(longerMatch.getId(), ranked.get(1).getId(), "Pro Max should rank second");
    }

    @Test
    @DisplayName("RelevanceRanker — ranks product matching more tokens higher for multi-word search")
    void testRelevanceRanker_MultiWordMatching() {
        Product fullMatch = new Product();
        fullMatch.setId(1L);
        fullMatch.setTitle("Men's Blue Formal Shirt");
        fullMatch.setColor("Blue");
        fullMatch.setQuantity(10);
        fullMatch.setCreatedAt(LocalDateTime.now());

        Product partialMatch = new Product();
        partialMatch.setId(2L);
        partialMatch.setTitle("Men's Black Casual Shirt");
        partialMatch.setColor("Black");
        partialMatch.setQuantity(10);
        partialMatch.setCreatedAt(LocalDateTime.now());

        SearchQueryParser.ParsedSearchQuery query = queryParser.parse("blue formal shirt");
        List<Product> ranked = relevanceRanker.rank(List.of(partialMatch, fullMatch), query);

        assertEquals(fullMatch.getId(), ranked.get(0).getId(), "Blue formal shirt should rank higher than black casual shirt");
    }

    @Test
    @DisplayName("RelevanceRanker — boosts in-stock product over out-of-stock product with same match")
    void testRelevanceRanker_InStockBoost() {
        Product inStock = new Product();
        inStock.setId(10L);
        inStock.setTitle("Wireless Bluetooth Earbuds");
        inStock.setQuantity(15);
        inStock.setCreatedAt(LocalDateTime.now().minusDays(1));

        Product outOfStock = new Product();
        outOfStock.setId(11L);
        outOfStock.setTitle("Wireless Bluetooth Earbuds");
        outOfStock.setQuantity(0);
        outOfStock.setCreatedAt(LocalDateTime.now());

        SearchQueryParser.ParsedSearchQuery query = queryParser.parse("wireless earbuds");
        List<Product> ranked = relevanceRanker.rank(List.of(outOfStock, inStock), query);

        assertEquals(inStock.getId(), ranked.get(0).getId(), "In-stock item should rank higher than out-of-stock item");
    }

    @Test
    @DisplayName("RelevanceRanker — recognizes brand match and category context")
    void testRelevanceRanker_BrandAndCategoryMatching() {
        Category laptopCat = new Category();
        laptopCat.setName("Electronics Gaming Laptops");
        laptopCat.setCategoryId("gaming_laptops");

        Product laptop = new Product();
        laptop.setId(4L);
        laptop.setTitle("HP OMEN Gaming Laptop 16 with RTX 5050");
        laptop.setBrand("HP");
        laptop.setCategory(laptopCat);
        laptop.setQuantity(5);
        laptop.setCreatedAt(LocalDateTime.now());

        SearchQueryParser.ParsedSearchQuery query = queryParser.parse("gaming laptop");
        double score = relevanceRanker.calculateScore(laptop, query);

        assertTrue(score > 60.0, "Score should reflect title match, category context, and in-stock bonus");
    }

    @Test
    @DisplayName("QueryParser — safely handles empty, blank, and null queries")
    void testQueryParser_EmptyAndNull() {
        assertTrue(queryParser.parse(null).isEmpty());
        assertTrue(queryParser.parse("").isEmpty());
        assertTrue(queryParser.parse("    ").isEmpty());
    }

    // ─── 4. Specification & Service Search Tests ─────────────────────────

    @Test
    @DisplayName("ProductSpecification — builds null-safe specification for empty tokens")
    void testProductSpecification_EmptyTokens() {
        assertNotNull(com.zosh.service.impl.ProductSpecification.search(null, true));
        assertNotNull(com.zosh.service.impl.ProductSpecification.search(List.of(), true));
        assertNotNull(com.zosh.service.impl.ProductSpecification.search(""));
    }

    @Test
    @DisplayName("ProductSpecification — builds multi-token specification with and/or strategy")
    void testProductSpecification_Tokens() {
        var specAll = com.zosh.service.impl.ProductSpecification.search(List.of("apple", "iphone"), true);
        var specAny = com.zosh.service.impl.ProductSpecification.search(List.of("apple", "iphone"), false);
        assertNotNull(specAll);
        assertNotNull(specAny);
    }

    @Test
    @DisplayName("SearchRelevanceRanker — breaks ties deterministically using createdAt DESC")
    void testRelevanceRanker_TieBreaker() {
        Product p1 = new Product();
        p1.setId(1L);
        p1.setTitle("Cotton Shirt");
        p1.setCreatedAt(LocalDateTime.now().minusHours(2));

        Product p2 = new Product();
        p2.setId(2L);
        p2.setTitle("Cotton Shirt");
        p2.setCreatedAt(LocalDateTime.now());

        SearchQueryParser.ParsedSearchQuery query = queryParser.parse("cotton shirt");
        List<Product> ranked = relevanceRanker.rank(List.of(p1, p2), query);

        assertEquals(2L, ranked.get(0).getId(), "Newer product p2 should rank first in tie-break");
    }
}
