package com.zosh.service.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Intelligent query parsing engine for ShopSphere e-commerce search.
 * Handles normalization, stop-word removal, natural price intent extraction,
 * and typo tolerance recovery.
 */
@Component
public class SearchQueryParser {

    private static final int MAX_QUERY_LENGTH = 150;

    // Common English stop words in e-commerce queries that add noise when matching
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "the", "a", "an", "and", "or", "for", "with", "in", "of", "by", "at", "to", "on", "from"
    ));

    // Natural language price regex patterns (e.g., "under 1500", "below 20000", "between 1000 and 5000", "above 500")
    private static final Pattern BETWEEN_PRICE_PATTERN = Pattern.compile(
            "\\b(?:between\\s+)?(?:rs\\.?|inr|₹)?\\s*(\\d+)\\s*(?:to|and|-)\\s*(?:rs\\.?|inr|₹)?\\s*(\\d+)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern UNDER_PRICE_PATTERN = Pattern.compile(
            "\\b(?:under|below|less\\s+than|upto|up\\s+to)\\s*(?:rs\\.?|inr|₹)?\\s*(\\d+)(?:\\s*(?:rs|inr|/-))?\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern ABOVE_PRICE_PATTERN = Pattern.compile(
            "\\b(?:above|over|more\\s+than)\\s*(?:rs\\.?|inr|₹)?\\s*(\\d+)(?:\\s*(?:rs|inr|/-))?\\b",
            Pattern.CASE_INSENSITIVE
    );

    private final CatalogDictionaryService dictionaryService;

    @Autowired
    public SearchQueryParser(CatalogDictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public static class ParsedSearchQuery {
        private final String rawQuery;
        private final String cleanQuery;
        private final String correctedQuery;
        private final List<String> tokens;
        private final Integer extractedMinPrice;
        private final Integer extractedMaxPrice;
        private final boolean corrected;

        public ParsedSearchQuery(String rawQuery, String cleanQuery, String correctedQuery,
                                 List<String> tokens, Integer extractedMinPrice,
                                 Integer extractedMaxPrice, boolean corrected) {
            this.rawQuery = rawQuery;
            this.cleanQuery = cleanQuery;
            this.correctedQuery = correctedQuery;
            this.tokens = tokens;
            this.extractedMinPrice = extractedMinPrice;
            this.extractedMaxPrice = extractedMaxPrice;
            this.corrected = corrected;
        }

        public String getRawQuery() { return rawQuery; }
        public String getCleanQuery() { return cleanQuery; }
        public String getCorrectedQuery() { return correctedQuery; }
        public List<String> getTokens() { return tokens; }
        public Integer getExtractedMinPrice() { return extractedMinPrice; }
        public Integer getExtractedMaxPrice() { return extractedMaxPrice; }
        public boolean isCorrected() { return corrected; }
        public boolean hasExtractedPrice() { return extractedMinPrice != null || extractedMaxPrice != null; }
        public boolean isEmpty() { return tokens.isEmpty(); }
    }

    /**
     * Parses, normalizes, extracts price intents, and checks for typos in the user query.
     *
     * @param rawQuery Raw search string from client
     * @return Fully parsed and structured search query
     */
    public ParsedSearchQuery parse(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return new ParsedSearchQuery("", "", "", List.of(), null, null, false);
        }

        // 1. Length guard to prevent malicious buffer/ReDoS issues
        String trimmed = rawQuery.trim();
        if (trimmed.length() > MAX_QUERY_LENGTH) {
            trimmed = trimmed.substring(0, MAX_QUERY_LENGTH).trim();
        }

        Integer extractedMinPrice = null;
        Integer extractedMaxPrice = null;

        // 2. Natural Price Extraction
        Matcher betweenMatcher = BETWEEN_PRICE_PATTERN.matcher(trimmed);
        if (betweenMatcher.find()) {
            try {
                int p1 = Integer.parseInt(betweenMatcher.group(1));
                int p2 = Integer.parseInt(betweenMatcher.group(2));
                extractedMinPrice = Math.min(p1, p2);
                extractedMaxPrice = Math.max(p1, p2);
                trimmed = trimmed.replace(betweenMatcher.group(0), " ");
            } catch (NumberFormatException ignored) {}
        } else {
            Matcher underMatcher = UNDER_PRICE_PATTERN.matcher(trimmed);
            if (underMatcher.find()) {
                try {
                    extractedMaxPrice = Integer.parseInt(underMatcher.group(1));
                    trimmed = trimmed.replace(underMatcher.group(0), " ");
                } catch (NumberFormatException ignored) {}
            }

            Matcher aboveMatcher = ABOVE_PRICE_PATTERN.matcher(trimmed);
            if (aboveMatcher.find()) {
                try {
                    extractedMinPrice = Integer.parseInt(aboveMatcher.group(1));
                    trimmed = trimmed.replace(aboveMatcher.group(0), " ");
                } catch (NumberFormatException ignored) {}
            }
        }

        // 3. Normalization: clean punctuation, collapse whitespace
        String normalized = trimmed
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.isEmpty()) {
            return new ParsedSearchQuery(rawQuery, "", "", List.of(), extractedMinPrice, extractedMaxPrice, false);
        }

        // 4. Tokenization and stop-word filtering
        String[] rawTokens = normalized.split("\\s+");
        List<String> meaningfulTokens = new ArrayList<>();

        for (String t : rawTokens) {
            if (t.length() > 0 && !STOP_WORDS.contains(t)) {
                meaningfulTokens.add(t);
            }
        }

        // If filtering stop words left nothing (e.g. query was just "for" or "in"), preserve the tokens
        if (meaningfulTokens.isEmpty()) {
            for (String t : rawTokens) {
                if (t.length() > 0) {
                    meaningfulTokens.add(t);
                }
            }
        }

        // 5. Typo detection and spelling correction
        List<String> correctedTokens = new ArrayList<>();
        boolean anyCorrection = false;

        for (String token : meaningfulTokens) {
            Optional<String> corrected = dictionaryService.findClosestTerm(token);
            if (corrected.isPresent() && !corrected.get().equalsIgnoreCase(token)) {
                correctedTokens.add(corrected.get());
                anyCorrection = true;
            } else {
                correctedTokens.add(token);
            }
        }

        String cleanQueryString = String.join(" ", meaningfulTokens);
        String correctedQueryString = String.join(" ", correctedTokens);

        return new ParsedSearchQuery(
                rawQuery,
                cleanQueryString,
                correctedQueryString,
                correctedTokens,
                extractedMinPrice,
                extractedMaxPrice,
                anyCorrection
        );
    }
}
