package com.zosh.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.zosh.model.Category;
import com.zosh.model.Product;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

/**
 * Production search specifications for ShopSphere products catalog.
 * Supports multi-token search across titles, brands, categories, descriptions,
 * and colors with distinct query execution.
 */
public class ProductSpecification {

    private ProductSpecification() {}

    /**
     * Builds a specification matching search tokens across all participating product fields.
     *
     * @param tokens List of normalized keywords
     * @param matchAll If true, requires every token to match (AND); if false, matches any token (OR)
     * @return JPA Specification for Product
     */
    public static Specification<Product> search(List<String> tokens, boolean matchAll) {
        return (root, cq, cb) -> {
            if (tokens == null || tokens.isEmpty()) {
                return cb.conjunction();
            }

            if (cq.getResultType() != Long.class && cq.getResultType() != long.class) {
                cq.distinct(true);
            }

            Join<Product, Category> category = root.join("category", JoinType.LEFT);
            Join<Category, Category> parent = category.join("parentCategory", JoinType.LEFT);
            Join<Category, Category> grandParent = parent.join("parentCategory", JoinType.LEFT);

            List<Predicate> tokenPredicates = new ArrayList<>();

            for (String token : tokens) {
                if (token == null || token.isBlank()) continue;
                String likePattern = "%" + token.toLowerCase(Locale.ROOT) + "%";

                Predicate tokenMatch = cb.or(
                        cb.like(cb.lower(root.get("title")), likePattern),
                        cb.like(cb.lower(root.get("brand")), likePattern),
                        cb.like(cb.lower(root.get("color")), likePattern),
                        cb.like(cb.lower(category.get("name")), likePattern),
                        cb.like(cb.lower(category.get("categoryId")), likePattern),
                        cb.like(cb.lower(parent.get("name")), likePattern),
                        cb.like(cb.lower(parent.get("categoryId")), likePattern),
                        cb.like(cb.lower(grandParent.get("name")), likePattern),
                        cb.like(cb.lower(grandParent.get("categoryId")), likePattern),
                        cb.like(cb.lower(root.get("description")), likePattern)
                );

                tokenPredicates.add(tokenMatch);
            }

            if (tokenPredicates.isEmpty()) {
                return cb.conjunction();
            }

            return matchAll
                    ? cb.and(tokenPredicates.toArray(new Predicate[0]))
                    : cb.or(tokenPredicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Backward-compatible overload accepting a raw query string.
     */
    public static Specification<Product> search(String query) {
        if (query == null || query.isBlank()) {
            return (root, cq, cb) -> cb.conjunction();
        }

        String normalized = query.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
        List<String> tokens = List.of(normalized.split(" "));
        return search(tokens, true);
    }
}
