package com.zosh.service.ai;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zosh.domain.ChatIntent;
import com.zosh.dto.chat.AiProductDto;
import com.zosh.dto.chat.ChatRequest;
import com.zosh.dto.chat.ChatResponse;
import com.zosh.dto.chat.StructuredShoppingRequest;
import com.zosh.model.Category;
import com.zosh.model.Product;
import com.zosh.model.chat.ChatSession;
import com.zosh.repository.chat.ChatMessageRepository;
import com.zosh.repository.chat.ChatSessionRepository;
import com.zosh.service.ProductService;
import com.zosh.service.UserService;
import com.zosh.service.ai.impl.ChatServiceImpl;
import com.zosh.service.ai.llm.GroqService;
import com.zosh.service.ai.tool.AiToolExecutor;

@ExtendWith(MockitoExtension.class)
class ChatAssistantOverhaulTest {

    private IntentClassificationService intentClassifier;
    private ContextResolutionService contextService;
    private ProductRankingService rankingService;

    @Mock private ChatSessionRepository sessionRepo;
    @Mock private ChatMessageRepository messageRepo;
    @Mock private GroqService groqService;
    @Mock private AiToolExecutor toolExecutor;
    @Mock private ChatRateLimiter rateLimiter;
    @Mock private UserService userService;
    @Mock private StorePolicyService policyService;
    @Mock private ProductService productService;

    private ChatServiceImpl chatService;

    @BeforeEach
    void setUp() {
        intentClassifier = new IntentClassificationService();
        contextService = new ContextResolutionService();
        rankingService = new ProductRankingService();

        chatService = new ChatServiceImpl(
                sessionRepo,
                messageRepo,
                groqService,
                toolExecutor,
                rateLimiter,
                userService,
                intentClassifier,
                contextService,
                policyService,
                new ObjectMapper()
        );
    }

    // ─── 1. Conversational & Non-Commerce Intent Tests ─────────────────────

    @Test
    @DisplayName("'okay' and acknowledgments classify as GENERAL_CONVERSATION and do not search products")
    void testAcknowledgmentIntents() {
        String[] acknowledgments = {"okay", "ok", "fine", "alright", "sure", "got it", "cool", "yes", "no"};
        for (String ack : acknowledgments) {
            ChatIntent intent = intentClassifier.classify(ack, null);
            assertEquals(ChatIntent.GENERAL_CONVERSATION, intent, "Failed for: " + ack);
            assertTrue(intentClassifier.isConversational(intent));
        }

        when(rateLimiter.isAllowed(anyString())).thenReturn(true);
        ChatSession session = new ChatSession("sess-1", null);
        when(sessionRepo.findBySessionToken("sess-1")).thenReturn(Optional.of(session));
        when(sessionRepo.save(any())).thenReturn(session);

        ChatResponse resp = chatService.chat(new ChatRequest("sess-1", "okay"), null);
        assertNotNull(resp);
        assertEquals(ChatIntent.GENERAL_CONVERSATION, resp.getIntent());
        assertFalse(resp.getMessage().contains("I couldn't find any products matching"));
        verify(toolExecutor, never()).executeTool(eq("search_products"), any(), any());
    }

    @Test
    @DisplayName("'what is react' classifies as GENERAL_QUESTION and does not search products")
    void testGeneralQuestionIntents() {
        ChatIntent intent = intentClassifier.classify("what is react", null);
        assertEquals(ChatIntent.GENERAL_QUESTION, intent);
        assertTrue(intentClassifier.isConversational(intent));

        when(rateLimiter.isAllowed(anyString())).thenReturn(true);
        ChatSession session = new ChatSession("sess-1", null);
        when(sessionRepo.findBySessionToken("sess-1")).thenReturn(Optional.of(session));
        when(sessionRepo.save(any())).thenReturn(session);

        ChatResponse resp = chatService.chat(new ChatRequest("sess-1", "what is react"), null);
        assertNotNull(resp);
        assertEquals(ChatIntent.GENERAL_QUESTION, resp.getIntent());
        assertTrue(resp.getMessage().toLowerCase().contains("react"));
        verify(toolExecutor, never()).executeTool(eq("search_products"), any(), any());
    }

    @Test
    @DisplayName("Single-word modifier 'higher' without session context requires clarification")
    void testAmbiguousModifierWithoutContext() {
        ChatIntent intent = intentClassifier.classify("higher", null);
        assertEquals(ChatIntent.CLARIFICATION_REQUIRED, intent);
        assertTrue(intentClassifier.isConversational(intent));

        when(rateLimiter.isAllowed(anyString())).thenReturn(true);
        ChatSession session = new ChatSession("sess-1", null);
        when(sessionRepo.findBySessionToken("sess-1")).thenReturn(Optional.of(session));
        when(sessionRepo.save(any())).thenReturn(session);

        ChatResponse resp = chatService.chat(new ChatRequest("sess-1", "higher"), null);
        assertNotNull(resp);
        assertEquals(ChatIntent.CLARIFICATION_REQUIRED, resp.getIntent());
        assertTrue(resp.getMessage().contains("clarify"));
        verify(toolExecutor, never()).executeTool(eq("search_products"), any(), any());
    }

    // ─── 2. Structured Shopping Request Extraction Tests ───────────────────

    @Test
    @DisplayName("'hey give me shirt price less than 700' extracts category=shirt and maxPrice=700")
    void testShirtUnder700Extraction() {
        StructuredShoppingRequest req = contextService.resolveStructuredShoppingRequest(
                "hey give me shirt price less than 700", null, ChatIntent.PRODUCT_SEARCH);

        assertEquals(700, req.getMaxPrice());
        assertEquals("shirt", req.getQuery().toLowerCase());
        assertNull(req.getMinPrice());
    }

    @Test
    @DisplayName("'give me shirt price is less than 500' extracts category=shirt and maxPrice=500")
    void testShirtLessThan500Extraction() {
        StructuredShoppingRequest req = contextService.resolveStructuredShoppingRequest(
                "give me shirt price is less than 500", null, ChatIntent.PRODUCT_SEARCH);

        assertEquals(500, req.getMaxPrice());
        assertEquals("shirt", req.getQuery().toLowerCase());
    }

    @Test
    @DisplayName("'give me phones' extracts category=electronics_smartphones")
    void testPhonesExtraction() {
        StructuredShoppingRequest req = contextService.resolveStructuredShoppingRequest(
                "give me phones", null, ChatIntent.PRODUCT_SEARCH);

        assertEquals("electronics_smartphones", req.getCategory());
        assertEquals("Smartphones", req.getCategoryName());
    }

    @Test
    @DisplayName("'i want to buy phone' extracts smartphones category")
    void testBuyPhoneExtraction() {
        StructuredShoppingRequest req = contextService.resolveStructuredShoppingRequest(
                "i want to buy phone", null, ChatIntent.PRODUCT_SEARCH);

        assertEquals("electronics_smartphones", req.getCategory());
    }

    @Test
    @DisplayName("'give me mobile apple' extracts brand=Apple and category=electronics_smartphones")
    void testAppleMobileExtraction() {
        StructuredShoppingRequest req = contextService.resolveStructuredShoppingRequest(
                "give me mobile apple", null, ChatIntent.PRODUCT_SEARCH);

        assertEquals("Apple", req.getBrand());
        assertEquals("electronics_smartphones", req.getCategory());
    }

    @Test
    @DisplayName("'give me t shirts' extracts category=men_tshirts")
    void testTShirtsExtraction() {
        StructuredShoppingRequest req = contextService.resolveStructuredShoppingRequest(
                "give me t shirts", null, ChatIntent.PRODUCT_SEARCH);

        assertEquals("men_tshirts", req.getCategory());
        assertEquals("T-Shirts", req.getCategoryName());
        assertEquals("T-Shirt", req.getQuery());
    }

    @Test
    @DisplayName("Natural price range 'phones between 20000 and 40000' extracts minPrice and maxPrice")
    void testPriceRangeExtraction() {
        StructuredShoppingRequest req = contextService.resolveStructuredShoppingRequest(
                "phones between 20000 and 40000", null, ChatIntent.PRODUCT_SEARCH);

        assertEquals(20000, req.getMinPrice());
        assertEquals(40000, req.getMaxPrice());
        assertEquals("electronics_smartphones", req.getCategory());
    }

    @Test
    @DisplayName("Color and size extraction works seamlessly with constraints")
    void testColorAndSizeExtraction() {
        StructuredShoppingRequest req = contextService.resolveStructuredShoppingRequest(
                "blue formal shirt in size L under 1500", null, ChatIntent.PRODUCT_SEARCH);

        assertEquals("Blue", req.getColor());
        assertEquals("L", req.getSize());
        assertEquals(1500, req.getMaxPrice());
        assertEquals("men_formal_shirts", req.getCategory());
    }

    // ─── 3. T-Shirt vs Generic Shirt Discrimination ────────────────────────

    @Test
    @DisplayName("Ranking service strictly ranks true T-Shirts above formal/casual shirts")
    void testTShirtRankingDiscrimination() {
        Category tShirtCat = new Category(); tShirtCat.setCategoryId("men_tshirts"); tShirtCat.setName("Men Tshirts");
        Category formalCat = new Category(); formalCat.setCategoryId("men_formal_shirts"); formalCat.setName("Men Formal Shirts");
        Category casualCat = new Category(); casualCat.setCategoryId("men_casual_shirts"); casualCat.setName("Men Casual Shirts");

        Product tShirt = new Product();
        tShirt.setId(1L);
        tShirt.setTitle("Men's Oversized Chill Out Graphic T-Shirt");
        tShirt.setCategory(tShirtCat);
        tShirt.setSellingPrice(650);
        tShirt.setQuantity(10);

        Product casualShirt = new Product();
        casualShirt.setId(2L);
        casualShirt.setTitle("Men's Regular Fit Solid Black Casual Shirt");
        casualShirt.setCategory(casualCat);
        casualShirt.setSellingPrice(799);
        casualShirt.setQuantity(10);

        Product formalShirt = new Product();
        formalShirt.setId(3L);
        formalShirt.setTitle("Men's Slim Fit Solid Teal Formal Shirt");
        formalShirt.setCategory(formalCat);
        formalShirt.setSellingPrice(750);
        formalShirt.setQuantity(10);

        List<Product> candidates = List.of(formalShirt, casualShirt, tShirt);

        List<AiProductDto> ranked = rankingService.rank(candidates, null, "t-shirt", "men_tshirts");

        assertFalse(ranked.isEmpty());
        assertEquals("Men's Oversized Chill Out Graphic T-Shirt", ranked.get(0).getTitle(),
                "T-Shirt must be ranked #1 when t-shirt is requested");
    }

    // ─── 4. Multi-Turn Context Refinement Tests ───────────────────────────

    @Test
    @DisplayName("Multi-turn: 'Suggest me shirt' -> 'formal' -> '1000' retains category and sets maxPrice")
    void testMultiTurnShirtRefinement() {
        ChatSession session = new ChatSession("session-turn", null);
        session.setLastCategory("men_formal_shirts");
        session.setLastSearchQuery("Formal Shirt");

        // User says "1000" as a standalone budget reply
        StructuredShoppingRequest req = contextService.resolveStructuredShoppingRequest(
                "1000", session, ChatIntent.PRODUCT_SEARCH);

        assertEquals("men_formal_shirts", req.getCategory());
        assertEquals(1000, req.getMaxPrice());
        assertEquals("Formal Shirt", req.getQuery());
        assertTrue(req.isFollowUpRefinement());
    }

    @Test
    @DisplayName("Multi-turn: 'show me phones' -> 'apple only' -> 'under 50000'")
    void testMultiTurnPhoneRefinement() {
        ChatSession session = new ChatSession("session-phone", null);
        session.setLastCategory("electronics_smartphones");
        session.setLastSearchQuery("smartphone");

        // Turn 2: "apple only"
        StructuredShoppingRequest turn2 = contextService.resolveStructuredShoppingRequest(
                "apple only", session, ChatIntent.PRODUCT_SEARCH);
        assertEquals("Apple", turn2.getBrand());
        assertEquals("electronics_smartphones", turn2.getCategory());

        session.setLastBrand(turn2.getBrand());

        // Turn 3: "under 50000"
        StructuredShoppingRequest turn3 = contextService.resolveStructuredShoppingRequest(
                "under 50000", session, ChatIntent.PRODUCT_SEARCH);
        assertEquals("Apple", turn3.getBrand());
        assertEquals("electronics_smartphones", turn3.getCategory());
        assertEquals(50000, turn3.getMaxPrice());
    }
}
