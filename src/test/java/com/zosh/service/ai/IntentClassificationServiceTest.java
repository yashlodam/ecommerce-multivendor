package com.zosh.service.ai;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zosh.domain.ChatIntent;
import com.zosh.model.chat.ChatSession;

class IntentClassificationServiceTest {

    private IntentClassificationService classifier;

    @BeforeEach
    void setUp() {
        classifier = new IntentClassificationService();
    }

    @Test
    @DisplayName("Greetings are classified as GREETING and identified as conversational")
    void testGreetings() {
        String[] greetings = {"Hi", "Hello", "Hey", "good morning", "Good evening", "Namaste", "hey there!"};
        for (String g : greetings) {
            ChatIntent intent = classifier.classify(g, null);
            assertEquals(ChatIntent.GREETING, intent, "Expected GREETING for: " + g);
            assertTrue(classifier.isConversational(intent));
        }
    }

    @Test
    @DisplayName("Mixed intent with greeting prefix classifies as commerce intent")
    void testMixedIntent() {
        assertEquals(ChatIntent.PRODUCT_SEARCH, classifier.classify("Hi, show me shirts", null));
        assertEquals(ChatIntent.PRODUCT_SEARCH, classifier.classify("Hello, I need a laptop under ₹50000", null));
        assertEquals(ChatIntent.PRODUCT_RECOMMENDATION, classifier.classify("Hey, which phone is best for gaming?", null));
        assertEquals(ChatIntent.CART_VIEW, classifier.classify("Hi, what's in my cart?", null));
    }

    @Test
    @DisplayName("stripGreetingPrefix cleanly removes greeting prefix for mixed queries")
    void testStripGreetingPrefix() {
        assertEquals("show me shirts", classifier.stripGreetingPrefix("Hi, show me shirts"));
        assertEquals("I need a laptop under 50000", classifier.stripGreetingPrefix("Hello, I need a laptop under 50000"));
        assertEquals("Hi", classifier.stripGreetingPrefix("Hi"));
    }

    @Test
    @DisplayName("General conversation, gratitude, farewell and help are classified properly")
    void testConversationalMetaIntents() {
        assertEquals(ChatIntent.GENERAL_CONVERSATION, classifier.classify("How are you?", null));
        assertEquals(ChatIntent.GRATITUDE, classifier.classify("Thanks a lot!", null));
        assertEquals(ChatIntent.GRATITUDE, classifier.classify("Thank you", null));
        assertEquals(ChatIntent.FAREWELL, classifier.classify("Bye", null));
        assertEquals(ChatIntent.FAREWELL, classifier.classify("Goodbye", null));
        assertEquals(ChatIntent.HELP, classifier.classify("Who are you?", null));
        assertEquals(ChatIntent.HELP, classifier.classify("I need help", null));
        assertEquals(ChatIntent.OFF_TOPIC, classifier.classify("Tell me a joke", null));
    }

    @Test
    @DisplayName("Cart and Order operations classify correctly")
    void testCartAndOrders() {
        assertEquals(ChatIntent.CART_VIEW, classifier.classify("What's in my cart?", null));
        assertEquals(ChatIntent.CART_ADD, classifier.classify("Add it to my cart", null));
        assertEquals(ChatIntent.CART_ADD, classifier.classify("Add the second one", null));
        assertEquals(ChatIntent.CART_CLEAR, classifier.classify("Clear my cart", null));
        assertEquals(ChatIntent.ORDER_TRACKING, classifier.classify("Where is my order?", null));
        assertEquals(ChatIntent.ORDER_HISTORY, classifier.classify("Show my orders", null));
        assertEquals(ChatIntent.ORDER_CANCEL, classifier.classify("Cancel my order", null));
    }

    @Test
    @DisplayName("Store policies classify correctly")
    void testStorePolicies() {
        assertEquals(ChatIntent.RETURN_POLICY, classifier.classify("What is your return policy?", null));
        assertEquals(ChatIntent.SHIPPING_INFO, classifier.classify("Do you offer free shipping?", null));
        assertEquals(ChatIntent.PAYMENT_INFO, classifier.classify("What payment methods are supported?", null));
    }

    @Test
    @DisplayName("Recommendations, comparisons, and variants classify correctly")
    void testProductIntents() {
        assertEquals(ChatIntent.PRODUCT_RECOMMENDATION, classifier.classify("Which shirt is best for an interview?", null));
        assertEquals(ChatIntent.PRODUCT_COMPARISON, classifier.classify("Compare these two products", null));
        assertEquals(ChatIntent.VARIANT_AVAILABILITY, classifier.classify("Do you have this in size L?", null));
    }

    @Test
    @DisplayName("Short follow-ups refine prior product search context")
    void testContextRefinements() {
        ChatSession session = new ChatSession();
        session.setLastSearchQuery("formal shirts");

        assertEquals(ChatIntent.PRODUCT_SEARCH, classifier.classify("blue", session));
        assertEquals(ChatIntent.PRODUCT_SEARCH, classifier.classify("under 1500", session));
        assertEquals(ChatIntent.PRODUCT_SEARCH, classifier.classify("size L", session));
        assertEquals(ChatIntent.PRODUCT_SEARCH, classifier.classify("cheaper", session));
    }
}
