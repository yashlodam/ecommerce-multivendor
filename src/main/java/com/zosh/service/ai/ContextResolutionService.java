package com.zosh.service.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.zosh.domain.ChatIntent;
import com.zosh.dto.chat.AiProductDto;
import com.zosh.dto.chat.StructuredShoppingRequest;
import com.zosh.model.chat.ChatSession;

/**
 * Enterprise context and entity resolution service for ShopSphere AI Shopping Assistant.
 * Extracts intent, categories, brands, price boundaries, sizes, colors, and keywords.
 * Maintains multi-turn conversational context across turns without leaking conversational fluff.
 */
@Service
public class ContextResolutionService {

    private static final Pattern ORDINAL_PATTERN = Pattern.compile(
            "\\b(first|second|third|fourth|fifth|sixth|1st|2nd|3rd|4th|5th|6th)\\b",
            Pattern.CASE_INSENSITIVE);

    // Matches price patterns like: "under 1500", "price less than 700", "price is less than 500", "upto 2000"
    private static final Pattern MAX_PRICE_PATTERN = Pattern.compile(
            "(?:price\\s*(?:is|was)?\\s*)?(?:under|below|less than|within|upto|up to|max|budget|<=?)\\s*(?:rs\\.?|inr|\\u20b9)?\\s*(\\d+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern MIN_PRICE_PATTERN = Pattern.compile(
            "(?:above|over|more than|greater than|>=?)\\s*(?:rs\\.?|inr|\\u20b9)?\\s*(\\d+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern BETWEEN_PRICE_PATTERN = Pattern.compile(
            "(?:between|from)\\s*(?:rs\\.?|inr|\\u20b9)?\\s*(\\d+)\\s*(?:and|to)\\s*(?:rs\\.?|inr|\\u20b9)?\\s*(\\d+)",
            Pattern.CASE_INSENSITIVE);

    // Standalone number (e.g. "1000", "500", "₹1500") following a conversational budget prompt
    private static final Pattern STANDALONE_PRICE_PATTERN = Pattern.compile(
            "^\\s*(?:rs\\.?|inr|\\u20b9)?\\s*(\\d{2,7})\\s*$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SIZE_PATTERN = Pattern.compile(
            "\\b(?:size\\s*)?\\b(XS|S|M|L|XL|XXL|2XL|3XL|XXXL|\\d{2,3}(?:GB|TB|mm|cm)?)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final String[] KNOWN_COLORS = {
            "red", "blue", "green", "black", "white", "yellow", "pink",
            "teal", "navy", "grey", "gray", "brown", "purple", "orange", "maroon", "lavender"
    };

    private static final String[] KNOWN_BRANDS = {
            "Apple", "OnePlus", "Google", "HP", "FireBolt", "Yash",
            "Samsung", "Nike", "Adidas", "Puma", "Boat", "Dell", "Lenovo", "Asus", "Sony"
    };

    // ─── Structured Shopping Request Resolution ────────────────────────────

    public StructuredShoppingRequest resolveStructuredShoppingRequest(String rawMessage, ChatSession session, ChatIntent intent) {
        StructuredShoppingRequest req = new StructuredShoppingRequest();
        req.setIntent(intent != null ? intent : ChatIntent.PRODUCT_SEARCH);

        String message = (rawMessage != null) ? rawMessage.trim() : "";
        String lower = message.toLowerCase(Locale.ROOT);

        // 1. Inherit prior session constraints if available
        if (session != null) {
            req.setCategory(session.getLastCategory());
            req.setBrand(session.getLastBrand());
            req.setColor(session.getLastColor());
            req.setSize(session.getLastSize());
            req.setMinPrice(session.getLastMinPrice());
            req.setMaxPrice(session.getLastMaxPrice());
            req.setQuery(session.getLastSearchQuery());
        }

        // 2. Price Extraction
        extractPriceConstraints(message, session, req);

        // 3. Color Extraction
        String detectedColor = extractColor(message);
        if (detectedColor != null) {
            req.setColor(detectedColor);
        }

        // 4. Size Extraction
        String detectedSize = extractSize(message);
        if (detectedSize != null) {
            req.setSize(detectedSize);
        }

        // 5. Brand Extraction
        String detectedBrand = extractBrand(message);
        if (detectedBrand != null) {
            req.setBrand(detectedBrand);
        }

        // 6. Category Normalization & Discrimination (T-Shirt vs. Shirt, Phones, Laptops, etc.)
        resolveCategoryAndKeywords(message, session, req);

        return req;
    }

    private void extractPriceConstraints(String message, ChatSession session, StructuredShoppingRequest req) {
        // Between pattern: "between 20000 and 40000"
        Matcher betweenM = BETWEEN_PRICE_PATTERN.matcher(message);
        if (betweenM.find()) {
            try {
                req.setMinPrice(Integer.parseInt(betweenM.group(1)));
                req.setMaxPrice(Integer.parseInt(betweenM.group(2)));
                return;
            } catch (NumberFormatException ignored) {}
        }

        // Max price pattern: "under 1500", "price less than 700", "price is less than 500"
        Matcher maxM = MAX_PRICE_PATTERN.matcher(message);
        if (maxM.find()) {
            try {
                req.setMaxPrice(Integer.parseInt(maxM.group(1)));
            } catch (NumberFormatException ignored) {}
        }

        // Min price pattern: "above 1000"
        Matcher minM = MIN_PRICE_PATTERN.matcher(message);
        if (minM.find()) {
            try {
                req.setMinPrice(Integer.parseInt(minM.group(1)));
            } catch (NumberFormatException ignored) {}
        }

        // Standalone number pattern: user replying "1000", "500", "₹1200"
        Matcher standaloneM = STANDALONE_PRICE_PATTERN.matcher(message);
        if (standaloneM.matches()) {
            try {
                req.setMaxPrice(Integer.parseInt(standaloneM.group(1)));
                req.setFollowUpRefinement(true);
            } catch (NumberFormatException ignored) {}
        }

        // Relative budget modifier: "cheaper"
        if (message.toLowerCase(Locale.ROOT).contains("cheaper") && req.getMaxPrice() != null) {
            req.setMaxPrice((int) (req.getMaxPrice() * 0.8));
            req.setFollowUpRefinement(true);
        }
    }

    private void resolveCategoryAndKeywords(String message, ChatSession session, StructuredShoppingRequest req) {
        String lower = message.toLowerCase(Locale.ROOT).trim();

        // ── T-Shirt vs. Shirt Handling ────────────────────────────────────
        // When user asks for "t-shirt", "t shirt", "t shirts", "tees":
        if (lower.matches(".*\\b(t\\s*-?\\s*shirts?|tees?)\\b.*")) {
            req.setCategory("men_tshirts");
            req.setCategoryName("T-Shirts");
            req.setQuery("T-Shirt");
            return;
        }

        // Formal shirt refinement (e.g. user says "formal" or "formal shirt")
        if (lower.matches(".*\\b(formal|dress\\s*shirt|office\\s*shirt)\\b.*")) {
            req.setCategory("men_formal_shirts");
            req.setCategoryName("Formal Shirts");
            req.setQuery("Formal Shirt");
            return;
        }

        // Casual shirt refinement (e.g. user says "casual" or "casual shirt")
        if (lower.matches(".*\\b(casual\\s*shirts?)\\b.*") ||
            (lower.contains("casual") && (session != null && "shirt".equalsIgnoreCase(session.getLastCategory())))) {
            req.setCategory("men_casual_shirts");
            req.setCategoryName("Casual Shirts");
            req.setQuery("Casual Shirt");
            return;
        }

        // Generic shirt (do not override if already specialized)
        if (lower.matches(".*\\b(shirts?)\\b.*")) {
            if (req.getCategory() == null || (!req.getCategory().contains("shirt") && !req.getCategory().equals("men_tshirts"))) {
                req.setCategory(null); // broad shirt search across formal, casual, t-shirts
                req.setCategoryName("Shirts");
                req.setQuery("shirt");
            }
            return;
        }

        // ── Smartphones / Mobiles ─────────────────────────────────────────
        if (lower.matches(".*\\b(phones?|mobiles?|smartphones?)\\b.*")) {
            req.setCategory("electronics_smartphones");
            req.setCategoryName("Smartphones");
            req.setQuery(req.hasBrand() ? req.getBrand() : "smartphone");
            return;
        }

        // ── Laptops ───────────────────────────────────────────────────────
        if (lower.matches(".*\\b(laptops?|gaming\\s*laptops?|notebooks?)\\b.*")) {
            req.setCategory("electronics_gaming_laptops");
            req.setCategoryName("Gaming Laptops");
            req.setQuery("laptop");
            return;
        }

        // ── Smart Watches ─────────────────────────────────────────────────
        if (lower.matches(".*\\b(smart\\s*watch(?:es)?|watches?|smartwatches?)\\b.*")) {
            req.setCategory("electronics_smart_watches");
            req.setCategoryName("Smart Watches");
            req.setQuery("smartwatch");
            return;
        }

        // ── Kurtas ────────────────────────────────────────────────────────
        if (lower.matches(".*\\b(kurtas?|kurtis?)\\b.*")) {
            req.setCategory("women_kurtas");
            req.setCategoryName("Women Kurtas");
            req.setQuery("kurta");
            return;
        }

        // ── Tops ──────────────────────────────────────────────────────────
        if (lower.matches(".*\\b(tops?|peplum)\\b.*")) {
            req.setCategory("women_tops");
            req.setCategoryName("Women Tops");
            req.setQuery("top");
            return;
        }

        // If the message is a refinement like "apple only", "blue", "size L", standalone price "1000"
        if (isRefinementOnly(lower)) {
            req.setFollowUpRefinement(true);
            // Retain previous query
            if (req.hasBrand() && (req.getQuery() == null || req.getQuery().isBlank() || req.getQuery().equalsIgnoreCase("phone") || req.getQuery().equalsIgnoreCase("smartphone"))) {
                req.setQuery(req.getBrand());
            }
            return;
        }

        // Fallback: extract clean search keywords
        String cleanedKeywords = extractKeywords(message);
        if (!cleanedKeywords.isBlank()) {
            req.setQuery(cleanedKeywords);
        } else if (req.getQuery() == null || req.getQuery().isBlank()) {
            req.setQuery("product");
        }
    }

    // ─── Attribute Extraction Helpers ─────────────────────────────────────

    public Integer extractBudget(String message) {
        if (message == null) return null;

        Matcher betweenM = BETWEEN_PRICE_PATTERN.matcher(message);
        if (betweenM.find()) {
            try { return Integer.parseInt(betweenM.group(2)); } catch (Exception ignored) {}
        }

        Matcher maxM = MAX_PRICE_PATTERN.matcher(message);
        if (maxM.find()) {
            try { return Integer.parseInt(maxM.group(1)); } catch (Exception ignored) {}
        }

        Matcher standaloneM = STANDALONE_PRICE_PATTERN.matcher(message);
        if (standaloneM.matches()) {
            try { return Integer.parseInt(standaloneM.group(1)); } catch (Exception ignored) {}
        }

        return null;
    }

    public String extractBrand(String message) {
        if (message == null) return null;
        String lower = message.toLowerCase(Locale.ROOT);
        for (String b : KNOWN_BRANDS) {
            if (lower.matches(".*\\b" + b.toLowerCase(Locale.ROOT) + "\\b.*")) {
                return b;
            }
        }
        return null;
    }

    public String extractSize(String message) {
        if (message == null) return null;
        Matcher m = SIZE_PATTERN.matcher(message);
        while (m.find()) {
            String candidate = m.group(1).toUpperCase(Locale.ROOT);
            if (candidate.matches("^(XS|S|M|L|XL|XXL|2XL|3XL|XXXL|\\d{2,3}(?:GB|TB)?)$")) {
                return candidate;
            }
        }
        return null;
    }

    public String extractColor(String message) {
        if (message == null) return null;
        String lower = message.toLowerCase(Locale.ROOT);
        for (String c : KNOWN_COLORS) {
            if (lower.matches(".*\\b" + c + "\\b.*")) {
                return Character.toUpperCase(c.charAt(0)) + c.substring(1);
            }
        }
        return null;
    }

    public boolean isRefinementOnly(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase(Locale.ROOT).trim();
        if (STANDALONE_PRICE_PATTERN.matcher(lower).matches()) return true;
        if (lower.matches("^[a-zA-Z]+\\s+only$")) return true; // e.g. "apple only"
        if (extractColor(lower) != null && lower.split("\\s+").length <= 2) return true;
        if (extractBudget(lower) != null && lower.split("\\s+").length <= 3) return true;
        if (extractSize(lower) != null && lower.split("\\s+").length <= 2) return true;
        if (lower.equals("cheaper") || lower.equals("more") || lower.equals("show more") ||
            lower.equals("formal") || lower.equals("casual") || lower.equals("higher") || lower.equals("lower")) {
            return true;
        }
        return false;
    }

    /**
     * Cleans natural language filler, greeting prefixes, price clauses, and stop words
     * to isolate true catalog search tokens.
     */
    public String extractKeywords(String text) {
        if (text == null || text.isBlank()) return "";

        // 1. Strip price clauses: "price is less than 500", "price less than 700", "under 1500"
        String cleaned = text.replaceAll("(?i)(?:price\\s*(?:is|was)?\\s*)?(?:under|below|less than|within|upto|up to|max|budget|above|over|between|from|to|<=?|>=?)\\s*(?:rs\\.?|inr|\\u20b9)?\\s*\\d+", " ");
        cleaned = cleaned.replaceAll("(?i)\\band\\s+\\d+", " ");

        // 2. Strip standalone price or currency
        cleaned = cleaned.replaceAll("(?i)\\b(?:rs\\.?|inr|\\u20b9)\\b", " ");

        // 3. Strip size clauses
        cleaned = cleaned.replaceAll("(?i)\\b(?:size|in)\\s+(?:XS|S|M|L|XL|XXL|2XL|3XL|XXXL|\\d+(?:GB|TB|mm|cm)?)\\b", " ");

        // 4. Strip conversational command wrappers
        cleaned = cleaned.replaceAll("(?i)\\b(which|what|who|where|how|is|are|was|were|be|been|a|an|the|for|of|in|on|at|to|by|with|about|into|best|good|better|show|me|find|get|give|look|looking|buy|want|need|please|can|you|recommend|suggest|suggestion|suggestions|item|items|product|products|one|ones|price|cost|rate|cheap|affordable|only)\\b", " ");

        // 5. Clean punctuation & normalize whitespace
        cleaned = cleaned.replaceAll("[^a-zA-Z0-9\\s-]", " ").replaceAll("\\s+", " ").trim();
        return cleaned;
    }

    // ─── Ordinal & Reference Resolution ───────────────────────────────────

    public Long resolveProductId(String message, ChatSession session) {
        if (message == null || session == null) return null;

        List<Long> lastIds = parseProductIdsJson(session.getLastProductIdsJson());
        Matcher m = ORDINAL_PATTERN.matcher(message);
        if (m.find()) {
            int idx = parseOrdinal(m.group(1));
            if (idx >= 0 && idx < lastIds.size()) {
                return lastIds.get(idx);
            }
        }

        if (refersToLastProduct(message) && session.getLastReferencedProductId() != null) {
            return session.getLastReferencedProductId();
        }

        return session.getLastReferencedProductId();
    }

    public boolean refersToLastProduct(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("this") || lower.contains("it") || lower.contains("that") ||
               lower.contains("the one") || lower.contains("same") || lower.contains("above");
    }

    public List<Long> parseProductIdsJson(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            List<Long> ids = new ArrayList<>();
            String clean = json.replace("[", "").replace("]", "").trim();
            if (clean.isEmpty()) return Collections.emptyList();
            for (String part : clean.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    ids.add(Long.parseLong(trimmed));
                }
            }
            return ids;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public String formatProductIdsJson(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < ids.size(); i++) {
            sb.append(ids.get(i));
            if (i < ids.size() - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    public Long resolveProductByOrdinal(String message, List<AiProductDto> lastResults) {
        if (lastResults == null || lastResults.isEmpty() || message == null) return null;
        Matcher m = ORDINAL_PATTERN.matcher(message);
        if (m.find()) {
            int idx = parseOrdinal(m.group(1));
            if (idx >= 0 && idx < lastResults.size()) {
                return lastResults.get(idx).getId();
            }
        }
        return null;
    }

    public Long resolveProductByColor(String message, List<AiProductDto> lastResults) {
        if (lastResults == null || lastResults.isEmpty() || message == null) return null;
        String color = extractColor(message);
        if (color == null) return null;

        String lowerColor = color.toLowerCase(Locale.ROOT);
        for (AiProductDto p : lastResults) {
            if (p.getColor() != null && p.getColor().toLowerCase(Locale.ROOT).contains(lowerColor)) {
                return p.getId();
            }
        }
        return null;
    }

    private int parseOrdinal(String ordinal) {
        return switch (ordinal.toLowerCase(Locale.ROOT)) {
            case "first", "1st" -> 0;
            case "second", "2nd" -> 1;
            case "third", "3rd" -> 2;
            case "fourth", "4th" -> 3;
            case "fifth", "5th" -> 4;
            case "sixth", "6th" -> 5;
            default -> -1;
        };
    }
}
