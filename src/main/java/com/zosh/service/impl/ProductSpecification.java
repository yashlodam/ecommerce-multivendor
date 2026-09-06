package com.zosh.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.zosh.model.Category;
import com.zosh.model.Product;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

public class ProductSpecification {

    private ProductSpecification() {}

    public static Specification<Product> search(String query) {

        return (root, cq, cb) -> {

            if (query == null || query.isBlank()) {
                return cb.conjunction();
            }

            if (cq.getResultType() != Long.class && cq.getResultType() != long.class) {
                cq.distinct(true);
            }

            String normalized = query
                    .toLowerCase()
                    .trim()
                    .replaceAll("\\s+", " ");

            Join<Product, Category> category =
                    root.join("category", JoinType.LEFT);

            Join<Category, Category> parent =
                    category.join("parentCategory", JoinType.LEFT);

            Join<Category, Category> grandParent =
                    parent.join("parentCategory", JoinType.LEFT);

            List<Predicate> finalPredicates = new ArrayList<>();

            String[] words = normalized.split(" ");

            for (String word : words) {

                String like = "%" + word + "%";

                Predicate p = cb.or(

                        cb.like(cb.lower(root.get("title")), like),

                        cb.like(cb.lower(root.get("description")), like),

                        cb.like(cb.lower(root.get("color")), like),

                        cb.like(cb.lower(category.get("name")), like),

                        cb.like(cb.lower(parent.get("name")), like),

                        cb.like(cb.lower(grandParent.get("name")), like)

                );

                finalPredicates.add(p);

            }

            return cb.and(finalPredicates.toArray(new Predicate[0]));

        };

    }

}