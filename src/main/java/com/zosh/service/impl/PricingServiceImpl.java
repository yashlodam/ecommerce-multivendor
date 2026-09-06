package com.zosh.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zosh.domain.DealType;
import com.zosh.domain.DiscountType;
import com.zosh.dto.pricing.ProductPricingDto;
import com.zosh.model.CartItem;
import com.zosh.model.Category;
import com.zosh.model.Deal;
import com.zosh.model.Product;
import com.zosh.model.ProductVariant;
import com.zosh.repository.DealRepository;
import com.zosh.service.PricingService;

@Service
@Transactional(readOnly = true)
public class PricingServiceImpl implements PricingService {

    private static final Logger log = LoggerFactory.getLogger(PricingServiceImpl.class);

    @Autowired
    private DealRepository dealRepository;

    @Override
    public ProductPricingDto calculateProductPricing(Product product, ProductVariant variant) {
        ProductPricingDto dto = new ProductPricingDto();

        if (product == null) {
            return dto;
        }

        dto.setProductId(product.getId());
        if (variant != null) {
            dto.setVariantId(variant.getId());
        }

        // 1. Determine base unit price and MRP
        int baseSelling = variant != null && variant.getSellingPrice() != null
                ? variant.getSellingPrice()
                : (product.getSellingPrice() != null ? product.getSellingPrice() : 0);

        int baseMrp = variant != null && variant.getMrpPrice() != null
                ? variant.getMrpPrice()
                : (product.getMrpPrice() != null ? product.getMrpPrice() : baseSelling);

        BigDecimal basePrice = BigDecimal.valueOf(baseSelling);
        BigDecimal mrpPrice = BigDecimal.valueOf(baseMrp);

        dto.setBasePrice(baseSelling);
        dto.setMrpPrice(baseMrp);

        if (basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            dto.setEffectivePrice(0);
            dto.setDiscountAmount(0);
            dto.setDiscountPercentage(0);
            dto.setDealActive(false);
            return dto;
        }

        // 2. Fetch candidate active deals
        LocalDateTime now = LocalDateTime.now();
        List<Deal> candidates = new ArrayList<>();

        // (a) Product-level deals
        if (product.getId() != null) {
            candidates.addAll(dealRepository.findActiveProductDeals(product.getId(), now));
        }

        // (b) Category-level deals (current, parent, and grandparent)
        Category cat = product.getCategory();
        while (cat != null) {
            if (cat.getCategoryId() != null && !cat.getCategoryId().isBlank()) {
                candidates.addAll(dealRepository.findActiveCategoryDeals(cat.getCategoryId().trim(), now));
            }
            cat = cat.getParentCategory();
        }

        // Smart category matching: alias/suffix matching (e.g. 'headphones' matching 'electronics_headphones')
        List<Deal> activeCatDeals = dealRepository.findAllActiveCategoryDeals(now);
        for (Deal d : activeCatDeals) {
            if (!candidates.contains(d) && doesProductMatchCategoryDeal(product, d)) {
                candidates.add(d);
            }
        }


        // (c) Seller-level deals
        if (product.getSeller() != null && product.getSeller().getId() != null) {
            candidates.addAll(dealRepository.findActiveSellerDeals(product.getSeller().getId(), now));
        }

        // 3. Evaluate each candidate deal and pick the best (maximum rupee savings)
        Deal bestDeal = null;
        BigDecimal bestDiscountAmount = BigDecimal.ZERO;

        for (Deal deal : candidates) {
            if (!deal.isCurrentlyActive()) {
                continue;
            }

            // Minimum item price condition
            if (deal.getMinOrderAmount() != null && basePrice.compareTo(deal.getMinOrderAmount()) < 0) {
                continue;
            }

            BigDecimal calculatedDiscount = calculateDiscountForDeal(deal, basePrice);

            if (calculatedDiscount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // Compare with current best discount
            if (calculatedDiscount.compareTo(bestDiscountAmount) > 0) {
                bestDiscountAmount = calculatedDiscount;
                bestDeal = deal;
            } else if (calculatedDiscount.compareTo(bestDiscountAmount) == 0 && bestDeal != null) {
                // Precedence tie-breaker: PRODUCT > CATEGORY > SELLER
                if (getDealTypePriority(deal.getDealType()) < getDealTypePriority(bestDeal.getDealType())) {
                    bestDeal = deal;
                }
            }
        }

        // 4. Compute final effective price and percentage
        if (bestDeal != null && bestDiscountAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal effectiveBigDecimal = basePrice.subtract(bestDiscountAmount).max(BigDecimal.ZERO);
            int effectivePrice = effectiveBigDecimal.setScale(0, RoundingMode.HALF_UP).intValue();
            int discountAmount = bestDiscountAmount.setScale(0, RoundingMode.HALF_UP).intValue();

            dto.setEffectivePrice(effectivePrice);
            dto.setDiscountAmount(discountAmount);
            dto.setDealActive(true);
            dto.setAppliedDealId(bestDeal.getId());
            dto.setAppliedDealTitle(bestDeal.getTitle());
            dto.setDealType(bestDeal.getDealType() != null ? bestDeal.getDealType().name() : null);
            dto.setDealEndsAt(bestDeal.getEndAt());

            // Compute total discount percentage against MRP
            if (mrpPrice.compareTo(BigDecimal.ZERO) > 0) {
                int totalPct = mrpPrice.subtract(effectiveBigDecimal)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(mrpPrice, 0, RoundingMode.HALF_UP)
                        .intValue();
                dto.setDiscountPercentage(Math.max(0, Math.min(99, totalPct)));
            } else {
                dto.setDiscountPercentage(0);
            }
        } else {
            // No active deal applies
            dto.setEffectivePrice(baseSelling);
            dto.setDiscountAmount(0);
            dto.setDealActive(false);

            if (baseMrp > baseSelling && baseMrp > 0) {
                int defaultPct = (int) Math.round(((double) (baseMrp - baseSelling) / baseMrp) * 100);
                dto.setDiscountPercentage(Math.max(0, Math.min(99, defaultPct)));
            } else {
                dto.setDiscountPercentage(0);
            }
        }

        return dto;
    }

    @Override
    public ProductPricingDto applyDealPricingToCartItem(CartItem item) {
        if (item == null || item.getProduct() == null) {
            return new ProductPricingDto();
        }

        ProductPricingDto pricing = calculateProductPricing(item.getProduct(), item.getVariant());
        int qty = item.getQuantity() > 0 ? item.getQuantity() : 1;

        item.setSellingPrice(pricing.getEffectivePrice() * qty);
        item.setMrpPrice(pricing.getMrpPrice() * qty);

        return pricing;
    }

    @Override
    public Deal findBestOrderDeal(BigDecimal cartSubtotal) {
        if (cartSubtotal == null || cartSubtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        List<Deal> orderDeals = dealRepository.findActiveOrderDeals(now);

        Deal bestOrderDeal = null;
        BigDecimal bestDiscount = BigDecimal.ZERO;

        for (Deal deal : orderDeals) {
            if (!deal.isCurrentlyActive()) {
                continue;
            }

            if (deal.getMinOrderAmount() != null && cartSubtotal.compareTo(deal.getMinOrderAmount()) < 0) {
                continue;
            }

            BigDecimal discount = calculateDiscountForDeal(deal, cartSubtotal);
            if (discount.compareTo(bestDiscount) > 0) {
                bestDiscount = discount;
                bestOrderDeal = deal;
            }
        }

        return bestOrderDeal;
    }

    @Override
    public BigDecimal calculateOrderDealDiscount(BigDecimal cartSubtotal) {
        Deal deal = findBestOrderDeal(cartSubtotal);
        if (deal == null) {
            return BigDecimal.ZERO;
        }
        return calculateDiscountForDeal(deal, cartSubtotal);
    }

    // ─── Helper Methods ─────────────────────────────────────────────────────────

    private BigDecimal calculateDiscountForDeal(Deal deal, BigDecimal baseAmount) {
        if (deal == null || baseAmount == null || baseAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = BigDecimal.ZERO;

        if (deal.getDiscountType() == DiscountType.PERCENTAGE) {
            BigDecimal rate = deal.getDiscountValue() != null ? deal.getDiscountValue() : BigDecimal.ZERO;
            discount = baseAmount.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            if (deal.getMaxDiscountAmount() != null && discount.compareTo(deal.getMaxDiscountAmount()) > 0) {
                discount = deal.getMaxDiscountAmount();
            }
        } else if (deal.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            discount = deal.getDiscountValue() != null ? deal.getDiscountValue() : BigDecimal.ZERO;
        }

        // Discount cannot exceed base price
        return discount.min(baseAmount);
    }

    private int getDealTypePriority(DealType type) {
        if (type == null) return 99;
        switch (type) {
            case PRODUCT:
                return 1;
            case CATEGORY:
                return 2;
            case SELLER:
                return 3;
            case ORDER:
                return 4;
            default:
                return 99;
        }
    }

    private boolean doesProductMatchCategoryDeal(Product product, Deal deal) {

        if (product == null || deal == null || deal.getDealType() != DealType.CATEGORY) {
            return false;
        }

        String dealSlug = deal.getCategorySlug();
        if ((dealSlug == null || dealSlug.isBlank()) && deal.getCategory() != null) {
            dealSlug = deal.getCategory().getCategoryId();
        }
        if (dealSlug == null || dealSlug.isBlank()) {
            return false;
        }

        String cleanDeal = dealSlug.trim().toLowerCase().replace("-", "_");
        String dealNoUnder = cleanDeal.replace("_", "");

        Category cat = product.getCategory();
        while (cat != null) {
            if (cat.getCategoryId() != null) {
                String cid = cat.getCategoryId().trim().toLowerCase().replace("-", "_");
                String cname = cat.getName() != null ? cat.getName().trim().toLowerCase() : "";
                String cidNoUnder = cid.replace("_", "");

                if (cid.equals(cleanDeal)
                        || cidNoUnder.equals(dealNoUnder)
                        || cid.endsWith("_" + cleanDeal)
                        || cleanDeal.endsWith("_" + cid)
                        || cid.replace("electronics_", "").replace("men_", "").replace("women_", "").replace("home_", "").equals(cleanDeal)
                        || cid.replace("electronics_", "").replace("men_", "").replace("women_", "").replace("home_", "").replace("_", "").equals(dealNoUnder)
                        || (cleanDeal.length() >= 4 && (cid.contains(cleanDeal) || cname.contains(cleanDeal)))) {
                    return true;
                }
            }
            cat = cat.getParentCategory();
        }

        return false;
    }
}

