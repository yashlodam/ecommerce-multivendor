package com.zosh.service.search;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.zosh.model.Category;
import com.zosh.repository.CategoryRepository;
import com.zosh.repository.ProductRepository;

/**
 * Maintains an in-memory dictionary of catalog terms (brands, categories, common merchandise
 * tokens) for fast sub-millisecond typo detection and spelling correction.
 */
@Service
public class CatalogDictionaryService {

    private static final Logger log = LoggerFactory.getLogger(CatalogDictionaryService.class);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    private final Set<String> dictionary = ConcurrentHashMap.newKeySet();

    // Foundational e-commerce terms common to ShopSphere merchandise
    private static final List<String> BASE_TERMS = Arrays.asList(
            "apple", "iphone", "samsung", "oneplus", "google", "pixel", "hp", "omen", "dell",
            "lenovo", "asus", "acer", "sony", "firebolt", "boat", "noise", "fastrack", "titan",
            "yash", "puma", "nike", "adidas", "zara", "h&m", "roadster", "hrx", "levis",
            "men", "man", "women", "woman", "boys", "boy", "girls", "girl", "kids", "kid", "unisex",
            "laptop", "laptops", "phone", "phones", "smartphone", "smartphones", "mobile", "mobiles",
            "shirt", "shirts", "tshirt", "tshirts", "kurta", "kurtas",
            "watch", "watches", "smartwatch", "earbuds", "earbud", "headphones", "headphone",
            "shoes", "shoe", "sneaker", "sneakers", "jeans", "jean", "trouser", "trousers",
            "saree", "sarees", "dress", "dresses", "top", "tops", "palazzo", "palazzos", "pants",
            "camera", "cameras", "speaker", "speakers", "television", "televisions", "tv",
            "formal", "casual", "printed", "oversized", "solid", "cotton", "denim", "silk",
            "gaming", "wireless", "bluetooth", "amoled", "display", "storage", "battery",
            "discount", "offer", "sale", "deals", "deal",
            "black", "white", "blue", "red", "green", "yellow", "teal", "pink", "purple", "grey", "gray"
    );

    @Autowired
    public CatalogDictionaryService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.dictionary.addAll(BASE_TERMS);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        refreshDictionary();
    }

    /**
     * Refreshes dynamic catalog vocabulary from the database.
     */
    public synchronized void refreshDictionary() {
        try {
            // Seed base merchandise terms
            dictionary.addAll(BASE_TERMS);

            // Seed real brands from active catalog
            List<String> brands = productRepository.findDistinctBrands();
            if (brands != null) {
                for (String b : brands) {
                    if (b != null && !b.isBlank()) {
                        tokenizeAndAdd(b);
                    }
                }
            }

            // Seed real categories from category tree
            List<Category> categories = categoryRepository.findAll();
            if (categories != null) {
                for (Category c : categories) {
                    if (c.getName() != null && !c.getName().isBlank()) {
                        tokenizeAndAdd(c.getName());
                    }
                    if (c.getCategoryId() != null && !c.getCategoryId().isBlank()) {
                        tokenizeAndAdd(c.getCategoryId().replace("_", " "));
                    }
                }
            }

            log.info("Catalog search dictionary initialized with {} terms", dictionary.size());
        } catch (Exception e) {
            log.warn("Non-critical: Failed to refresh search catalog dictionary from database: {}", e.getMessage());
        }
    }

    private void tokenizeAndAdd(String text) {
        String clean = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ");
        for (String token : clean.split("\\s+")) {
            if (token.length() >= 2) {
                dictionary.add(token);
            }
        }
    }

    public Set<String> getDictionary() {
        return Collections.unmodifiableSet(dictionary);
    }

    /**
     * Finds the closest valid catalog dictionary term within Damerau-Levenshtein distance limit.
     *
     * @param token Word to check
     * @return Corrected term if a high-confidence match is found, otherwise empty.
     */
    public Optional<String> findClosestTerm(String token) {
        if (token == null) return Optional.empty();
        String cleanToken = token.trim().toLowerCase(Locale.ROOT);
        if (cleanToken.length() < 3) {
            return Optional.empty();
        }

        // 1. High-confidence explicit domain aliases & common typographical transpositions
        if ("kurti".equals(cleanToken) || "kurtis".equals(cleanToken)) {
            return Optional.of("kurta");
        }
        if ("ipone".equals(cleanToken) || "iphne".equals(cleanToken) || "iphon".equals(cleanToken)) {
            return Optional.of("iphone");
        }
        if ("smasung".equals(cleanToken) || "samsng".equals(cleanToken)) {
            return Optional.of("samsung");
        }
        if ("onepls".equals(cleanToken) || "onepluse".equals(cleanToken)) {
            return Optional.of("oneplus");
        }
        if ("lapotp".equals(cleanToken) || "laptp".equals(cleanToken) || "leptop".equals(cleanToken)) {
            return Optional.of("laptop");
        }
        if ("shrit".equals(cleanToken) || "shrt".equals(cleanToken)) {
            return Optional.of("shirt");
        }

        // 2. If already in dictionary, no correction needed
        if (dictionary.contains(cleanToken)) {
            return Optional.of(cleanToken);
        }

        // 3. Short words (< 4 chars) like "men", "red", "tee" should NOT be fuzzy-matched
        if (cleanToken.length() < 4) {
            return Optional.empty();
        }

        int maxDistance = cleanToken.length() >= 6 ? 2 : 1;
        String bestMatch = null;
        int bestDistance = Integer.MAX_VALUE;

        for (String candidate : dictionary) {
            // First letter MUST match to avoid spurious phonetic drifts
            if (candidate.charAt(0) != cleanToken.charAt(0)) {
                continue;
            }

            // Optimization: Skip candidates with length difference greater than max distance
            if (Math.abs(candidate.length() - cleanToken.length()) > maxDistance) {
                continue;
            }

            int dist = damerauLevenshteinDistance(cleanToken, candidate);
            if (dist <= maxDistance && dist < bestDistance) {
                bestDistance = dist;
                bestMatch = candidate;
                if (dist == 1) {
                    break; // Early exit on 1-edit match
                }
            }
        }

        return Optional.ofNullable(bestMatch);
    }

    /**
     * Computes Damerau-Levenshtein distance supporting insertions, deletions, substitutions, and transpositions.
     */
    private int damerauLevenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        int[][] d = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) d[i][0] = i;
        for (int j = 0; j <= len2; j++) d[0][j] = j;

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                d[i][j] = Math.min(
                        Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1),
                        d[i - 1][j - 1] + cost
                );

                // Transposition check
                if (i > 1 && j > 1 &&
                        s1.charAt(i - 1) == s2.charAt(j - 2) &&
                        s1.charAt(i - 2) == s2.charAt(j - 1)) {
                    d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + cost);
                }
            }
        }

        return d[len1][len2];
    }
}
