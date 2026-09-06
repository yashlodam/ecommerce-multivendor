package com.zosh.service.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.zosh.dto.chat.AiProductDto;
import com.zosh.dto.chat.AiVariantDto;
import com.zosh.model.Product;
import com.zosh.model.ProductVariant;

@Service
public class ProductRankingService {

    public AiProductDto toDto(Product p) {
        if (p == null) return null;

        AiProductDto dto = new AiProductDto();
        dto.setId(p.getId());
        dto.setTitle(p.getTitle());
        dto.setDescription(p.getDescription());
        dto.setBrand(p.getBrand());
        dto.setColor(p.getColor());
        if (p.getCategory() != null) {
            dto.setCategoryName(p.getCategory().getName() != null ? p.getCategory().getName() : p.getCategory().getCategoryId());
        }
        dto.setMrpPrice(p.getMrpPrice());
        dto.setSellingPrice(p.getSellingPrice());
        dto.setDiscountPercent(p.getDiscountPercent());
        dto.setQuantity(p.getQuantity() != null ? p.getQuantity() : 0);
        dto.setInStock(dto.getQuantity() > 0);
        dto.setRating(p.getNumRatings() != null && p.getNumRatings() > 0 ? 4.5 : 4.0);
        dto.setNumRatings(p.getNumRatings() != null ? p.getNumRatings() : 0);

        if (p.getSeller() != null) {
            dto.setSellerName(p.getSeller().getSellerName());
        }

        if (p.getImages() != null) {
            dto.setImages(new ArrayList<>(p.getImages()));
        }

        if (p.getVariants() != null && !p.getVariants().isEmpty()) {
            List<AiVariantDto> vList = p.getVariants().stream()
                    .map(this::toVariantDto)
                    .collect(Collectors.toList());
            dto.setVariants(vList);
        }

        return dto;
    }

    public AiVariantDto toVariantDto(ProductVariant v) {
        if (v == null) return null;
        return new AiVariantDto(
                v.getId(),
                v.getVariantName(),
                v.getMrpPrice(),
                v.getSellingPrice(),
                v.getDiscountPercent(),
                v.getQuantity(),
                v.isDefault()
        );
    }

    public List<AiProductDto> rank(List<Product> products, Integer maxBudget, String queryKeywords) {
        if (products == null || products.isEmpty()) {
            return new ArrayList<>();
        }

        return products.stream()
                .map(this::toDto)
                .sorted(Comparator.comparingDouble((AiProductDto p) -> score(p, maxBudget, queryKeywords)).reversed())
                .limit(6)
                .collect(Collectors.toList());
    }

    private double score(AiProductDto p, Integer maxBudget, String queryKeywords) {
        double score = 0.0;

        // 1. In-stock bonus (+50 points)
        if (p.isInStock()) {
            score += 50.0;
        }

        // 2. Budget adherence (+40 points if within budget)
        if (maxBudget != null && maxBudget > 0) {
            if (p.getSellingPrice() != null && p.getSellingPrice() <= maxBudget) {
                score += 40.0;
                // Extra bonus for great value relative to budget
                double budgetRatio = (double) p.getSellingPrice() / maxBudget;
                score += (1.0 - budgetRatio) * 10.0;
            } else {
                score -= 30.0; // Penalize over-budget items
            }
        }

        // 3. Discount bonus (+ up to 15 points)
        if (p.getDiscountPercent() != null && p.getDiscountPercent() > 0) {
            score += Math.min(15.0, p.getDiscountPercent() * 0.3);
        }

        // 4. Rating bonus (+ up to 20 points)
        if (p.getRating() != null) {
            score += p.getRating() * 4.0;
        }

        // 5. Keyword match in title/brand (+20 points)
        if (queryKeywords != null && !queryKeywords.isBlank()) {
            String lower = queryKeywords.toLowerCase();
            if (p.getTitle() != null && p.getTitle().toLowerCase().contains(lower)) {
                score += 20.0;
            }
            if (p.getBrand() != null && p.getBrand().toLowerCase().contains(lower)) {
                score += 15.0;
            }
        }

        return score;
    }
}
