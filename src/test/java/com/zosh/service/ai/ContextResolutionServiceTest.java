package com.zosh.service.ai;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zosh.dto.chat.AiProductDto;

class ContextResolutionServiceTest {

    private ContextResolutionService contextService;

    @BeforeEach
    void setUp() {
        contextService = new ContextResolutionService();
    }

    @Test
    @DisplayName("resolveProductByOrdinal resolves first, second, 3rd, etc.")
    void testResolveOrdinal() {
        AiProductDto p1 = new AiProductDto(); p1.setId(101L);
        AiProductDto p2 = new AiProductDto(); p2.setId(102L);
        AiProductDto p3 = new AiProductDto(); p3.setId(103L);
        List<AiProductDto> list = List.of(p1, p2, p3);

        assertEquals(101L, contextService.resolveProductByOrdinal("Show me the first one", list));
        assertEquals(102L, contextService.resolveProductByOrdinal("I like the second item", list));
        assertEquals(103L, contextService.resolveProductByOrdinal("Tell me about the 3rd product", list));
        assertNull(contextService.resolveProductByOrdinal("Give me something else", list));
    }

    @Test
    @DisplayName("resolveProductByColor finds matching candidate by color keyword")
    void testResolveColor() {
        AiProductDto p1 = new AiProductDto(); p1.setId(1L); p1.setColor("Teal");
        AiProductDto p2 = new AiProductDto(); p2.setId(2L); p2.setColor("Black");
        List<AiProductDto> list = List.of(p1, p2);

        assertEquals(1L, contextService.resolveProductByColor("What about the teal shirt?", list));
        assertEquals(2L, contextService.resolveProductByColor("I prefer the black one", list));
        assertNull(contextService.resolveProductByColor("Do you have red?", list));
    }

    @Test
    @DisplayName("extractBudget parses various natural language budget expressions")
    void testExtractBudget() {
        assertEquals(1500, contextService.extractBudget("Find shirts under 1500"));
        assertEquals(2000, contextService.extractBudget("Show phones below Rs. 2000"));
        assertEquals(5000, contextService.extractBudget("Within ₹5000"));
        assertEquals(800, contextService.extractBudget("upto 800"));
        assertNull(contextService.extractBudget("Show me white shirts"));
    }

    @Test
    @DisplayName("extractSize parses size tags")
    void testExtractSize() {
        assertEquals("L", contextService.extractSize("Do you have this in size L?"));
        assertEquals("M", contextService.extractSize("Add to cart in size M"));
        assertEquals("XL", contextService.extractSize("size XL please"));
        assertNull(contextService.extractSize("What colors are available?"));
    }

    @Test
    @DisplayName("refersToLastProduct accurately detects pronouns and referential terms")
    void testRefersToLastProduct() {
        assertTrue(contextService.refersToLastProduct("Is it available in blue?"));
        assertTrue(contextService.refersToLastProduct("Add this to my cart"));
        assertTrue(contextService.refersToLastProduct("Tell me more about that"));
        assertFalse(contextService.refersToLastProduct("Search for running shoes"));
    }
}
