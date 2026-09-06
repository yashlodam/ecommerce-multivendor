package com.zosh.service.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.zosh.dto.chat.AiProductDto;
import com.zosh.model.chat.ChatSession;

@Service
public class ContextResolutionService {

    private static final Pattern ORDINAL_PATTERN = Pattern.compile(
            "\\b(first|second|third|fourth|fifth|sixth|1st|2nd|3rd|4th|5th|6th)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern BUDGET_PATTERN = Pattern.compile(
            "(?:under|below|less than|within|upto|up to|max|budget|<=?)\\s*(?:rs\\.?|inr|\\u20b9)?\\s*(\\d+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SIZE_PATTERN = Pattern.compile(
            "\\b(?:size|in)?\\s*\\b(XS|S|M|L|XL|XXL|XXXL|\\d{2,3}(?:GB|TB|mm|cm)?)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final String[] KNOWN_COLORS = {
            "red", "blue", "green", "black", "white", "yellow", "pink",
            "teal", "navy", "grey", "gray", "brown", "purple", "orange", "maroon"
    };

    // ─── Ordinal & Reference Resolution ───────────────────────────────────

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

    public Long resolveProductId(String message, ChatSession session) {
        if (message == null || session == null) return null;

        // 1. Try ordinal from last product list (e.g. "the second one")
        List<Long> lastIds = parseProductIdsJson(session.getLastProductIdsJson());
        Matcher m = ORDINAL_PATTERN.matcher(message);
        if (m.find()) {
            int idx = parseOrdinal(m.group(1));
            if (idx >= 0 && idx < lastIds.size()) {
                return lastIds.get(idx);
            }
        }

        // 2. Try pronoun reference (e.g. "this", "it", "that")
        if (refersToLastProduct(message) && session.getLastReferencedProductId() != null) {
            return session.getLastReferencedProductId();
        }

        return session.getLastReferencedProductId();
    }

    public Long resolveProductByColor(String message, List<AiProductDto> lastResults) {
        if (lastResults == null || lastResults.isEmpty() || message == null) return null;
        String color = extractColor(message);
        if (color == null) return null;

        String lowerColor = color.toLowerCase();
        for (AiProductDto p : lastResults) {
            if (p.getColor() != null && p.getColor().toLowerCase().contains(lowerColor)) {
                return p.getId();
            }
        }
        return null;
    }

    // ─── Attribute Extraction ─────────────────────────────────────────────

    public Integer extractBudget(String message) {
        if (message == null) return null;
        Matcher m = BUDGET_PATTERN.matcher(message);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public String extractSize(String message) {
        if (message == null) return null;
        Matcher m = SIZE_PATTERN.matcher(message);
        while (m.find()) {
            String candidate = m.group(1).toUpperCase();
            // Don't mistake words like "IN", "AN", "AM" for sizes
            if (candidate.matches("^(XS|S|M|L|XL|XXL|XXXL|\\d{2,3}(?:GB|TB)?)$")) {
                return candidate;
            }
        }
        return null;
    }

    public String extractColor(String message) {
        if (message == null) return null;
        String lower = message.toLowerCase();
        for (String c : KNOWN_COLORS) {
            if (lower.matches(".*\\b" + c + "\\b.*")) {
                // Capitalize first letter
                return Character.toUpperCase(c.charAt(0)) + c.substring(1);
            }
        }
        return null;
    }

    public boolean refersToLastProduct(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase();
        return lower.contains("this") || lower.contains("it") || lower.contains("that") ||
               lower.contains("the one") || lower.contains("same") || lower.contains("above");
    }

    // ─── Multi-turn Search Criteria Refinement ────────────────────────────

    public static class SearchCriteria {
        public String query;
        public String category;
        public Integer minPrice;
        public Integer maxPrice;
        public String color;
        public String size;
        public String brand;
    }

    public SearchCriteria resolveSearchCriteria(String message, ChatSession session) {
        SearchCriteria criteria = new SearchCriteria();

        // 1. Start with prior session criteria if present
        if (session != null) {
            criteria.query = session.getLastSearchQuery();
            criteria.category = session.getLastCategory();
            criteria.minPrice = session.getLastMinPrice();
            criteria.maxPrice = session.getLastMaxPrice();
            criteria.color = session.getLastColor();
            criteria.size = session.getLastSize();
            criteria.brand = session.getLastBrand();
        }

        // 2. Extract new budget
        Integer newBudget = extractBudget(message);
        if (newBudget != null) {
            criteria.maxPrice = newBudget;
        } else if (message.toLowerCase().contains("cheaper") && criteria.maxPrice != null) {
            // "cheaper" reduces previous budget by 20%
            criteria.maxPrice = (int) (criteria.maxPrice * 0.8);
        }

        // 3. Extract new color
        String newColor = extractColor(message);
        if (newColor != null) {
            criteria.color = newColor;
        }

        // 4. Extract new size
        String newSize = extractSize(message);
        if (newSize != null) {
            criteria.size = newSize;
        }

        // 5. Clean new keyword query
        String cleanedKeywords = extractKeywords(message);
        if (!cleanedKeywords.isBlank() && !isRefinementOnly(message)) {
            // New explicit product search replacing old query
            criteria.query = cleanedKeywords;
        } else if (criteria.query == null || criteria.query.isBlank()) {
            criteria.query = cleanedKeywords.isBlank() ? "shirt" : cleanedKeywords;
        }

        return criteria;
    }

    public boolean isRefinementOnly(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase().trim();
        // If message is just a color, budget, or size, it's a refinement
        if (extractColor(lower) != null && lower.split("\\s+").length <= 2) return true;
        if (extractBudget(lower) != null && lower.split("\\s+").length <= 3) return true;
        if (extractSize(lower) != null && lower.split("\\s+").length <= 2) return true;
        if (lower.equals("cheaper") || lower.equals("more") || lower.equals("show more")) return true;
        return false;
    }

    public String extractKeywords(String text) {
        if (text == null || text.isBlank()) return "";
        String cleaned = text.replaceAll("(?i)(?:under|below|less than|within|upto|up to|max|budget|<=?)\\s*(?:rs\\.?|inr|\\u20b9)?\\s*\\d+", "");
        cleaned = cleaned.replaceAll("(?i)\\b(?:size|in)\\s+(?:XS|S|M|L|XL|XXL|XXXL|\\d+(?:GB|TB|mm|cm)?)\\b", "");
        cleaned = cleaned.replaceAll("(?i)\\b(which|what|who|where|how|is|are|was|were|be|been|a|an|the|for|of|in|on|at|to|by|with|about|into|best|good|better|show|me|find|get|look|looking|buy|want|need|please|can|you|give|recommend|suggestion|suggestions|item|items|product|products|one|ones)\\b", "");
        cleaned = cleaned.replaceAll("[^a-zA-Z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
        return cleaned;
    }

    // ─── Product IDs JSON Serialization ───────────────────────────────────

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

    private int parseOrdinal(String ordinal) {
        return switch (ordinal.toLowerCase()) {
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
