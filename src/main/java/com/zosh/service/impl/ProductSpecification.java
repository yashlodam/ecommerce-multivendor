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
import jakarta.persistence.criteria.Root;

/**
 * Production search specifications for ShopSphere products catalog.
 * Supports multi-token search across titles, brands, categories, descriptions,
 * and colors with join reuse and query deduplication.
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

            Join<Product, Category> category = getOrCreateCategoryJoin(root);
            Join<Category, Category> parent = getOrCreateParentJoin(category);
            Join<Category, Category> grandParent = getOrCreateParentJoin(parent);

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

    @SuppressWarnings("unchecked")
    public static Join<Product, Category> getOrCreateCategoryJoin(Root<Product> root) {
        for (Join<Product, ?> j : root.getJoins()) {
            if ("category".equals(j.getAttribute().getName())) {
                return (Join<Product, Category>) j;
            }
        }
        return root.join("category", JoinType.LEFT);
    }

    @SuppressWarnings("unchecked")
    public static Join<Category, Category> getOrCreateParentJoin(Join<?, Category> categoryJoin) {
        for (Join<Category, ?> j : categoryJoin.getJoins()) {
            if ("parentCategory".equals(j.getAttribute().getName())) {
                return (Join<Category, Category>) j;
            }
        }
        return categoryJoin.join("parentCategory", JoinType.LEFT);
    }
}
