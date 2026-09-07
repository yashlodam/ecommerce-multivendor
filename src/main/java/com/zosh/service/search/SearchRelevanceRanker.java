package com.zosh.service.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.zosh.model.Category;
import com.zosh.model.Product;

/**
 * High-performance relevance scoring engine for ShopSphere e-commerce search.
 * Computes deterministic multi-factor relevance scores prioritizing exact titles,
 * prefixes, brand alignments, category contexts, and inventory availability.
 */
@Component
public class SearchRelevanceRanker {

    /**
     * Sorts products in descending order of relevance score.
     * Preserves stable sorting for ties using createdAt DESC and id DESC.
     */
    public List<Product> rank(List<Product> products, SearchQueryParser.ParsedSearchQuery parsedQuery) {
        if (products == null || products.isEmpty()) {
            return new ArrayList<>();
        }
        if (parsedQuery == null || parsedQuery.isEmpty()) {
            return new ArrayList<>(products);
        }

        List<Product> list = new ArrayList<>(products);
        list.sort((p1, p2) -> {
            double score1 = calculateScore(p1, parsedQuery);
            double score2 = calculateScore(p2, parsedQuery);

            int cmp = Double.compare(score2, score1);
            if (cmp != 0) return cmp;

            // Tie breaker 1: newer products first
            if (p1.getCreatedAt() != null && p2.getCreatedAt() != null) {
                int dateCmp = p2.getCreatedAt().compareTo(p1.getCreatedAt());
                if (dateCmp != 0) return dateCmp;
            }

            // Tie breaker 2: id DESC
            Long id1 = p1.getId() != null ? p1.getId() : 0L;
            Long id2 = p2.getId() != null ? p2.getId() : 0L;
            return Long.compare(id2, id1);
        });

        return list;
    }

    /**
     * Calculates the composite relevance score for a product.
     */
    public double calculateScore(Product product, SearchQueryParser.ParsedSearchQuery parsedQuery) {
        if (product == null || parsedQuery == null || parsedQuery.isEmpty()) {
            return 0.0;
        }

        double score = 0.0;

        String title = product.getTitle() != null ? product.getTitle().toLowerCase(Locale.ROOT) : "";
        String brand = product.getBrand() != null ? product.getBrand().toLowerCase(Locale.ROOT) : "";
        String color = product.getColor() != null ? product.getColor().toLowerCase(Locale.ROOT) : "";
        String description = product.getDescription() != null ? product.getDescription().toLowerCase(Locale.ROOT) : "";

        Category category = product.getCategory();
        String catName = category != null && category.getName() != null ? category.getName().toLowerCase(Locale.ROOT) : "";
        String catId = category != null && category.getCategoryId() != null ? category.getCategoryId().toLowerCase(Locale.ROOT).replace("_", " ") : "";

        String fullCleanQuery = parsedQuery.getCleanQuery().toLowerCase(Locale.ROOT);
        String fullCorrectedQuery = parsedQuery.getCorrectedQuery().toLowerCase(Locale.ROOT);

        // 1. EXACT TITLE MATCH (+100 points)
        if (title.equals(fullCleanQuery) || title.equals(fullCorrectedQuery)) {
            score += 100.0;
        }
        // 2. TITLE STARTS WITH QUERY (+60 points)
        else if (title.startsWith(fullCleanQuery) || title.startsWith(fullCorrectedQuery)) {
            score += 60.0;
        }
        // 3. FULL PHRASE CONTAINED IN TITLE (+40 points)
        else if (title.contains(fullCleanQuery) || title.contains(fullCorrectedQuery)) {
            score += 40.0;
        }

        // Title density bonus (rewards cleaner, more direct title matches over long titles)
        if (!fullCleanQuery.isEmpty() && title.contains(fullCleanQuery)) {
            double density = (double) fullCleanQuery.length() / Math.max(fullCleanQuery.length(), title.length());
            score += density * 20.0;
        }

        // 4. BRAND MATCH (+35 points for exact, +20 points for partial)
        if (!brand.isEmpty()) {
            if (brand.equals(fullCleanQuery) || brand.equals(fullCorrectedQuery)) {
                score += 35.0;
            } else if (brand.contains(fullCleanQuery) || brand.contains(fullCorrectedQuery)) {
                score += 20.0;
            }
        }

        // 5. CATEGORY CONTEXT MATCH (+25 points)
        if (!catName.isEmpty() && (catName.contains(fullCleanQuery) || catName.contains(fullCorrectedQuery))) {
            score += 25.0;
        } else if (!catId.isEmpty() && (catId.contains(fullCleanQuery) || catId.contains(fullCorrectedQuery))) {
            score += 25.0;
        }

        // 6. INDIVIDUAL TOKEN MATCHES
        List<String> tokens = parsedQuery.getTokens();
        int matchedTokensInTitle = 0;
        int matchedTokensAnywhere = 0;

        for (String token : tokens) {
            boolean matchedThisToken = false;

            if (title.contains(token)) {
                score += 15.0;
                matchedTokensInTitle++;
                matchedThisToken = true;
            }

            if (!brand.isEmpty() && brand.contains(token)) {
                score += 15.0;
                matchedThisToken = true;
            }

            if (!color.isEmpty() && color.contains(token)) {
                score += 10.0;
                matchedThisToken = true;
            }

            if ((!catName.isEmpty() && catName.contains(token)) || (!catId.isEmpty() && catId.contains(token))) {
                score += 10.0;
                matchedThisToken = true;
            }

            if (!description.isEmpty() && description.contains(token)) {
                score += 3.0;
                matchedThisToken = true;
            }

            if (matchedThisToken) {
                matchedTokensAnywhere++;
            }
        }

        // Token completion bonus: reward matching ALL tokens in title
        if (!tokens.isEmpty() && matchedTokensInTitle == tokens.size()) {
            score += 25.0;
        } else if (!tokens.isEmpty() && matchedTokensAnywhere == tokens.size()) {
            score += 15.0;
        }

        // 7. IN-STOCK BONUS (+10 points)
        int stock = product.getQuantity() != null ? product.getQuantity() : 0;
        if (stock > 0) {
            score += 10.0;
        }

        // 8. RATING / POPULARITY BOOST (up to +5 points)
        if (product.getNumRatings() != null && product.getNumRatings() > 0) {
            score += Math.min(5.0, product.getNumRatings() * 0.5);
        }

        return score;
    }
}
