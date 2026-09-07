package com.zosh.service.ai;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.zosh.domain.ChatIntent;
import com.zosh.model.chat.ChatSession;

@Service
public class IntentClassificationService {

    // ─── Pattern Definitions ──────────────────────────────────────────────

    // Greetings
    private static final Pattern GREETING_ONLY_PATTERN = Pattern.compile(
            "^\\s*(hi|hello|hey|hey there|hiya|howdy|good morning|good afternoon|good evening|greetings|namaste|hola)\\s*[!.,?]*\\s*$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern GREETING_PREFIX_PATTERN = Pattern.compile(
            "^\\s*(hi|hello|hey|hey there|good morning|good evening|good afternoon|namaste)\\s*[,!.-]?\\s*",
            Pattern.CASE_INSENSITIVE);

    // Conversational acknowledgments ("okay", "alright", "yes", "no")
    private static final Pattern ACKNOWLEDGMENT_PATTERN = Pattern.compile(
            "^\\s*(ok|okay|k|alright|fine|sure|got it|cool|sounds good|yes|yeah|yup|yep|no|nope|nah|noted)\\s*[!.,?]*\\s*$",
            Pattern.CASE_INSENSITIVE);

    // Gratitude
    private static final Pattern GRATITUDE_PATTERN = Pattern.compile(
            "\\b(thank you|thanks|thx|thank u|appreciate it|many thanks|thanks a lot|great help|awesome thanks)\\b",
            Pattern.CASE_INSENSITIVE);

    // Farewell
    private static final Pattern FAREWELL_PATTERN = Pattern.compile(
            "^\\s*(bye|goodbye|see you|see ya|cya|have a good day|good night|take care|bye bye)\\s*[!.,]*\\s*$",
            Pattern.CASE_INSENSITIVE);

    // Help & Bot Capabilities
    private static final Pattern HELP_PATTERN = Pattern.compile(
            "\\b(help|i need help|can you help me|what can you do|who are you|how does this work|capabilities|guide me|what are you)\\b",
            Pattern.CASE_INSENSITIVE);

    // Off-topic (jokes, poems, weather, songs)
    private static final Pattern OFF_TOPIC_PATTERN = Pattern.compile(
            "\\b(tell me a joke|sing a song|weather|who is the president|write code|write poem)\\b",
            Pattern.CASE_INSENSITIVE);

    // General Chit-chat & Social
    private static final Pattern CONVERSATION_PATTERN = Pattern.compile(
            "\\b(how are you|how's it going|how r u|what's up|how are things|are you a bot|are you human|are you ai)\\b",
            Pattern.CASE_INSENSITIVE);

    // Ambiguous single-word utterances ("higher", "lower", "more", "cheaper")
    private static final Pattern AMBIGUOUS_MODIFIER_PATTERN = Pattern.compile(
            "^\\s*(higher|lower|more|cheaper|expensive|less|another|different|next|options)\\s*[!.,?]*\\s*$",
            Pattern.CASE_INSENSITIVE);

    // Deals & Promotions
    private static final Pattern DEAL_PATTERN = Pattern.compile(
            "\\b(deals?|discounts?|offers?|today's deals|today's offers|sales?|promo codes?|coupons?)\\b",
            Pattern.CASE_INSENSITIVE);

    // Category Browsing
    private static final Pattern CATEGORY_BROWSE_PATTERN = Pattern.compile(
            "\\b(categories|browse categories|show categories|what categories|list categories|all categories)\\b",
            Pattern.CASE_INSENSITIVE);

    // Cart operations
    private static final Pattern CART_CLEAR_PATTERN = Pattern.compile(
            "\\b(clear cart|clear my cart|empty cart|empty my cart|remove everything from cart)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern CART_REMOVE_PATTERN = Pattern.compile(
            "\\b(remove from cart|delete from cart|take out of cart|remove this item|remove item)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern CART_UPDATE_PATTERN = Pattern.compile(
            "\\b(change quantity|update quantity|increase quantity|decrease quantity|set quantity)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern CART_ADD_PATTERN = Pattern.compile(
            "\\b(add to cart|add it to my cart|add this to cart|add to my cart|put in cart|buy this|add the (?:first|second|third|1st|2nd|3rd|one|item))\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern CART_VIEW_PATTERN = Pattern.compile(
            "\\b(what's in my cart|show my cart|view cart|check cart|check my cart|open cart|my cart|cart items|show cart)\\b",
            Pattern.CASE_INSENSITIVE);

    // Wishlist
    private static final Pattern WISHLIST_PATTERN = Pattern.compile(
            "\\b(wishlist|my wishlist|add to wishlist|save for later|favorite)\\b",
            Pattern.CASE_INSENSITIVE);

    // Orders
    private static final Pattern ORDER_CANCEL_PATTERN = Pattern.compile(
            "\\b(cancel order|cancel my order|cancel order #?\\d+)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern ORDER_TRACK_PATTERN = Pattern.compile(
            "\\b(where is my order|track order|track my order|tracking|when will my order arrive|delivery status|order status|where is order)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern ORDER_HISTORY_PATTERN = Pattern.compile(
            "\\b(my orders|show orders|show my orders|order history|past orders|what did i order|previous orders|latest order)\\b",
            Pattern.CASE_INSENSITIVE);

    // Policies
    private static final Pattern RETURN_POLICY_PATTERN = Pattern.compile(
            "\\b(return policy|returns|how do i return|return window|can i return|exchange policy|refund|refunds|money back)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SHIPPING_POLICY_PATTERN = Pattern.compile(
            "\\b(shipping|delivery charges|free shipping|delivery policy|how long does delivery take|estimated delivery|dispatch)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PAYMENT_POLICY_PATTERN = Pattern.compile(
            "\\b(payment methods|how can i pay|payment options|cod|cash on delivery|accepted payments|upi|razorpay)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern FAQ_PATTERN = Pattern.compile(
            "\\b(store policy|policies|faq|authenticity|genuine|guarantee|warranty|are products original)\\b",
            Pattern.CASE_INSENSITIVE);

    // Seller & Reviews
    private static final Pattern SELLER_PATTERN = Pattern.compile(
            "\\b(who is selling|seller info|seller details|about the seller|is this seller verified|who sells this|seller name)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern REVIEW_PATTERN = Pattern.compile(
            "\\b(rating|ratings|reviews|customer reviews|what do customers say|how many stars|is it well reviewed)\\b",
            Pattern.CASE_INSENSITIVE);

    // Comparative & Recommendation
    private static final Pattern COMPARISON_PATTERN = Pattern.compile(
            "\\b(compare|which is better|which is cheaper|which has better|difference between|compare these|compare them)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern RECOMMENDATION_PATTERN = Pattern.compile(
            "\\b(which (?:shirt|phone|laptop|shoe|item|product|one) is best|what do you recommend|which one should i buy|best for (?:an? )?(?:interview|college|office|gaming|running|gift)|top pick|suggest the best)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern VARIANT_PATTERN = Pattern.compile(
            "\\b(do you have (?:this|it) in|available in size|is size|in size|in stock in|check size|what sizes)\\b",
            Pattern.CASE_INSENSITIVE);

    // General Technical & Factual Questions ("what is react", "what is java", "who is...")
    private static final Pattern GENERAL_QUESTION_PATTERN = Pattern.compile(
            "^\\s*(what is|what's|who is|who was|who's|where is|explain|tell me about|how does|why is|difference between|capital of)\\s+([a-zA-Z0-9\\s]+?)\\s*[?]?\\s*$",
            Pattern.CASE_INSENSITIVE);

    // ─── Classification Method ─────────────────────────────────────────────

    public ChatIntent classify(String rawMessage, ChatSession session) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return ChatIntent.UNKNOWN;
        }

        String trimmed = rawMessage.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);

        // 1. Pure Conversational Acknowledgments (e.g. "okay", "fine", "cool", "yes")
        if (ACKNOWLEDGMENT_PATTERN.matcher(trimmed).matches()) {
            return ChatIntent.GENERAL_CONVERSATION;
        }

        // 2. Greetings & Farewells
        if (GREETING_ONLY_PATTERN.matcher(trimmed).matches()) {
            return ChatIntent.GREETING;
        }

        if (FAREWELL_PATTERN.matcher(trimmed).matches()) {
            return ChatIntent.FAREWELL;
        }

        if (GRATITUDE_PATTERN.matcher(lower).find() && !hasCommerceAction(lower)) {
            return ChatIntent.GRATITUDE;
        }

        if (HELP_PATTERN.matcher(lower).find() && !hasCommerceAction(lower)) {
            return ChatIntent.HELP;
        }

        // 3. Off-Topic & General Conversation
        if (OFF_TOPIC_PATTERN.matcher(lower).find() && !hasCommerceAction(lower)) {
            return ChatIntent.OFF_TOPIC;
        }

        if (CONVERSATION_PATTERN.matcher(lower).find() && !hasCommerceAction(lower)) {
            return ChatIntent.GENERAL_CONVERSATION;
        }

        // 4. Ambiguous modifier follow-ups ("higher", "lower", "cheaper")
        if (AMBIGUOUS_MODIFIER_PATTERN.matcher(trimmed).matches()) {
            if (session == null || (session.getLastSearchQuery() == null && session.getLastMaxPrice() == null)) {
                return ChatIntent.CLARIFICATION_REQUIRED;
            }
            return ChatIntent.PRODUCT_SEARCH;
        }

        // 5. Strip greeting prefix if mixed intent ("Hi, show me shirts" -> "show me shirts")
        String stripped = stripGreetingPrefix(trimmed);
        String strippedLower = stripped.toLowerCase(Locale.ROOT);

        // 6. Store Policy Actions (Check policies BEFORE general question!)
        if (RETURN_POLICY_PATTERN.matcher(strippedLower).find()) return ChatIntent.RETURN_POLICY;
        if (SHIPPING_POLICY_PATTERN.matcher(strippedLower).find()) return ChatIntent.SHIPPING_INFO;
        if (PAYMENT_POLICY_PATTERN.matcher(strippedLower).find()) return ChatIntent.PAYMENT_INFO;
        if (FAQ_PATTERN.matcher(strippedLower).find()) return ChatIntent.STORE_POLICY;

        // 7. Cart & Wishlist Actions
        if (WISHLIST_PATTERN.matcher(strippedLower).find()) return ChatIntent.WISHLIST_ACTION;
        if (CART_CLEAR_PATTERN.matcher(strippedLower).find()) return ChatIntent.CART_CLEAR;
        if (CART_REMOVE_PATTERN.matcher(strippedLower).find()) return ChatIntent.CART_REMOVE;
        if (CART_UPDATE_PATTERN.matcher(strippedLower).find()) return ChatIntent.CART_UPDATE;
        if (CART_ADD_PATTERN.matcher(strippedLower).find()) return ChatIntent.CART_ADD;
        if (CART_VIEW_PATTERN.matcher(strippedLower).find()) return ChatIntent.CART_VIEW;

        // 8. Order Actions
        if (ORDER_CANCEL_PATTERN.matcher(strippedLower).find()) return ChatIntent.ORDER_CANCEL;
        if (ORDER_TRACK_PATTERN.matcher(strippedLower).find()) return ChatIntent.ORDER_TRACKING;
        if (ORDER_HISTORY_PATTERN.matcher(strippedLower).find()) return ChatIntent.ORDER_HISTORY;

        // 9. Seller & Reviews
        if (SELLER_PATTERN.matcher(strippedLower).find()) return ChatIntent.SELLER_INFO;
        if (REVIEW_PATTERN.matcher(strippedLower).find()) return ChatIntent.REVIEW_INFO;

        // 10. Recommendations & Comparisons
        if (RECOMMENDATION_PATTERN.matcher(strippedLower).find()) return ChatIntent.PRODUCT_RECOMMENDATION;
        if (COMPARISON_PATTERN.matcher(strippedLower).find()) return ChatIntent.PRODUCT_COMPARISON;
        if (VARIANT_PATTERN.matcher(strippedLower).find()) return ChatIntent.VARIANT_AVAILABILITY;

        // 11. Category Browsing & Deals
        if (CATEGORY_BROWSE_PATTERN.matcher(strippedLower).find()) {
            return ChatIntent.CATEGORY_BROWSE;
        }
        if (DEAL_PATTERN.matcher(strippedLower).find() && !hasCommerceProductKeyword(strippedLower)) {
            return ChatIntent.DEAL_SEARCH;
        }

        // 12. General Questions (e.g. "what is react", "what is java", "who is...")
        Matcher generalQMatcher = GENERAL_QUESTION_PATTERN.matcher(trimmed);
        if (generalQMatcher.matches()) {
            String topic = generalQMatcher.group(2).trim().toLowerCase(Locale.ROOT);
            if (!isCommerceTopic(topic)) {
                return ChatIntent.GENERAL_QUESTION;
            }
        }

        // 13. Price-Driven Search
        if (isPriceDrivenSearch(strippedLower)) {
            return ChatIntent.PRICE_FILTER_SEARCH;
        }

        // 14. Gibberish Check
        if (isGibberish(strippedLower)) {
            return ChatIntent.CLARIFICATION_REQUIRED;
        }

        // 15. Contextual Follow-up or Standard Product Search
        return ChatIntent.PRODUCT_SEARCH;
    }

    // ─── Helper Methods ───────────────────────────────────────────────────

    public String stripGreetingPrefix(String text) {
        if (text == null) return "";
        Matcher m = GREETING_PREFIX_PATTERN.matcher(text);
        if (m.find()) {
            String rest = text.substring(m.end()).trim();
            if (!rest.isEmpty()) {
                return rest;
            }
        }
        return text.trim();
    }

    public boolean isConversational(ChatIntent intent) {
        return intent == ChatIntent.GREETING ||
               intent == ChatIntent.GENERAL_CONVERSATION ||
               intent == ChatIntent.GENERAL_QUESTION ||
               intent == ChatIntent.GRATITUDE ||
               intent == ChatIntent.FAREWELL ||
               intent == ChatIntent.HELP ||
               intent == ChatIntent.OFF_TOPIC ||
               intent == ChatIntent.CLARIFICATION_REQUIRED;
    }

    private boolean isCommerceTopic(String topic) {
        return topic.contains("price") || topic.contains("shirt") || topic.contains("phone") ||
               topic.contains("laptop") || topic.contains("watch") || topic.contains("apple") ||
               topic.contains("kurta") || topic.contains("cart") || topic.contains("order") ||
               topic.contains("product") || topic.contains("cost");
    }

    private boolean hasCommerceAction(String lower) {
        return lower.contains("show") || lower.contains("find") || lower.contains("buy") ||
               lower.contains("shirt") || lower.contains("phone") || lower.contains("laptop") ||
               lower.contains("cart") || lower.contains("order") || lower.contains("policy") ||
               lower.contains("price") || lower.contains("cost") || lower.contains("track") ||
               lower.contains("return") || lower.contains("recommend") || lower.contains("under") ||
               lower.contains("kurta") || lower.contains("watch");
    }

    private boolean hasCommerceProductKeyword(String lower) {
        return lower.contains("shirt") || lower.contains("phone") || lower.contains("laptop") ||
               lower.contains("watch") || lower.contains("kurta") || lower.contains("top") ||
               lower.contains("apple") || lower.contains("samsung") || lower.contains("shoes");
    }

    private boolean isPriceDrivenSearch(String lower) {
        return lower.matches(".*\\b(under|below|less than|within|upto|up to|between)\\s*\\d+.*") &&
               (lower.contains("product") || lower.contains("item") || lower.contains("anything") ||
                lower.contains("show") || lower.contains("find"));
    }

    private boolean isGibberish(String lower) {
        if (lower.matches("^(qwerty|asdfgh|zxcvbn|xyzabc|blah|asdf|qwer|poiuy)+.*$")) {
            return true;
        }
        if (lower.length() > 5 && !lower.matches(".*[aeiouy].*")) {
            return true;
        }
        return false;
    }
}
