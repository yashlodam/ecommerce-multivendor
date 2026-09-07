package com.zosh.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zosh.domain.DealType;
import com.zosh.domain.DiscountType;
import com.zosh.domain.HomeCategorySection;
import com.zosh.dto.deal.DealRequest;
import com.zosh.dto.deal.DealResponse;
import com.zosh.exceptions.ResourceNotFoundException;
import com.zosh.model.Deal;
import com.zosh.model.HomeCategory;
import com.zosh.model.Product;
import com.zosh.model.Seller;
import com.zosh.repository.DealRepository;
import com.zosh.repository.HomeCategoryRepository;
import com.zosh.repository.ProductRepository;
import com.zosh.domain.NotificationType;
import com.zosh.repository.SellerRepository;
import com.zosh.service.DealService;
import com.zosh.service.NotificationService;

@Service
public class DealServiceImpl implements DealService {

    private static final Logger log = LoggerFactory.getLogger(DealServiceImpl.class);

    @Autowired
    private DealRepository dealRepository;

    @Autowired
    private HomeCategoryRepository homeCategoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SellerRepository sellerRepository;

    @Autowired
    private NotificationService notificationService;

    // ─── Legacy methods ────────────────────────────────────────────────────────

    @Override
    public List<Deal> getDeals() {
        return dealRepository.findAll();
    }

    @Override
    @Transactional
    public Deal createDeal(Deal deal) {
        if (deal == null) {
            throw new IllegalArgumentException("Deal cannot be null");
        }
        if (deal.getCategory() == null) {
            throw new IllegalArgumentException("Category is required for deal");
        }

        HomeCategory category = null;

        // 1. Try finding by numeric ID if provided
        if (deal.getCategory().getId() != null) {
            category = homeCategoryRepository.findById(deal.getCategory().getId()).orElse(null);
        }

        // 2. Try finding by categoryId slug string
        if (category == null && deal.getCategory().getCategoryId() != null) {
            String slug = deal.getCategory().getCategoryId().trim();
            if (!slug.isEmpty()) {
                category = homeCategoryRepository.findByCategoryId(slug).orElse(null);
            }
        }

        // 3. If still not found, create HomeCategory dynamically
        if (category == null && deal.getCategory().getCategoryId() != null) {
            String slug = deal.getCategory().getCategoryId().trim();
            if (!slug.isEmpty()) {
                HomeCategory newCat = new HomeCategory();
                newCat.setCategoryId(slug);
                newCat.setName(slug.replace('_', ' ').replace('-', ' '));
                newCat.setSection(HomeCategorySection.DEALS);
                newCat.setImage("https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=400&q=80");
                category = homeCategoryRepository.save(newCat);
            }
        }

        if (category == null) {
            Long catId = deal.getCategory().getId();
            String slug = deal.getCategory().getCategoryId();
            throw new ResourceNotFoundException("HomeCategory", "identifier", catId != null ? catId.toString() : slug);
        }

        // 4. UPSERT: If a deal already exists for this category, update discount
        Optional<Deal> existingDealOpt = dealRepository.findByCategory(category);
        if (existingDealOpt.isPresent()) {
            Deal existing = existingDealOpt.get();
            existing.setDiscount(deal.getDiscount());
            existing.setTitle(deal.getTitle() != null ? deal.getTitle() : existing.getTitle());
            return dealRepository.save(existing);
        }

        Deal newDeal = new Deal();
        newDeal.setCategory(category);
        newDeal.setCategorySlug(category.getCategoryId());
        newDeal.setDiscount(deal.getDiscount());
        newDeal.setTitle(deal.getTitle() != null ? deal.getTitle() : category.getName() + " Flash Deal");
        newDeal.setDealType(DealType.CATEGORY);
        newDeal.setDiscountType(DiscountType.PERCENTAGE);
        newDeal.setStartAt(LocalDateTime.now());
        newDeal.setEndAt(LocalDateTime.now().plusMonths(6));

        return dealRepository.save(newDeal);
    }

    @Override
    @Transactional
    public Deal updateDeal(Deal deal, Long id) {
        Deal existingDeal = dealRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deal", "id", id));

        if (deal.getDiscount() != null) {
            existingDeal.setDiscount(deal.getDiscount());
        }
        if (deal.getTitle() != null && !deal.getTitle().isBlank()) {
            existingDeal.setTitle(deal.getTitle());
        }
        if (deal.getCategory() != null) {
            HomeCategory category = null;
            if (deal.getCategory().getId() != null) {
                category = homeCategoryRepository.findById(deal.getCategory().getId()).orElse(null);
            }
            if (category == null && deal.getCategory().getCategoryId() != null) {
                category = homeCategoryRepository.findByCategoryId(deal.getCategory().getCategoryId()).orElse(null);
            }
            if (category != null) {
                existingDeal.setCategory(category);
                existingDeal.setCategorySlug(category.getCategoryId());
            }
        }

        return dealRepository.save(existingDeal);
    }

    @Override
    @Transactional
    public void deleteDeal(Long id) {
        Deal deal = dealRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deal", "id", id));
        dealRepository.delete(deal);
    }

    // ─── Enterprise Deal Management ──────────────────────────────────────────

    @Override
    @Transactional
    public DealResponse createDealFromRequest(DealRequest req, Long sellerId, boolean isAdmin) {
        if (req == null) {
            throw new IllegalArgumentException("Deal request cannot be null.");
        }

        if (req.getDealType() == null) {
            if (req.getCategorySlug() != null || req.getCategoryId() != null) {
                req.setDealType(DealType.CATEGORY);
            } else {
                req.setDealType(DealType.PRODUCT);
            }
        }

        if (req.getTitle() == null || req.getTitle().isBlank()) {
            if (req.getCategorySlug() != null) {
                req.setTitle(req.getCategorySlug() + " Flash Deal");
            } else {
                req.setTitle("Special Promotion");
            }
        }

        if (req.getDiscountValue() == null || req.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Discount value must be greater than 0.");
        }

        if (req.getDiscountType() == DiscountType.PERCENTAGE && req.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Percentage discount cannot exceed 100%.");
        }

        LocalDateTime startAt = req.getStartAt() != null ? req.getStartAt() : LocalDateTime.now().minusMinutes(2);
        LocalDateTime endAt = req.getEndAt() != null ? req.getEndAt() : startAt.plusMonths(1);

        if (endAt.isBefore(startAt)) {
            throw new IllegalArgumentException("End date must be after start date.");
        }

        Deal deal = new Deal();
        deal.setTitle(req.getTitle().trim());
        deal.setDescription(req.getDescription());
        deal.setDealType(req.getDealType() != null ? req.getDealType() : DealType.PRODUCT);
        deal.setDiscountType(req.getDiscountType() != null ? req.getDiscountType() : DiscountType.PERCENTAGE);
        deal.setDiscountValue(req.getDiscountValue());
        deal.setMaxDiscountAmount(req.getMaxDiscountAmount());
        deal.setMinOrderAmount(req.getMinOrderAmount());
        deal.setStartAt(startAt);
        deal.setEndAt(endAt);
        deal.setActive(req.getActive() != null ? req.getActive() : true);
        deal.setUsageLimit(req.getUsageLimit());

        // Ownership and authorization enforcement
        if (!isAdmin) {
            // Sellers can ONLY create deals for products they own or their own seller store
            if (sellerId == null) {
                throw new AccessDeniedException("Seller context required.");
            }

            Seller seller = sellerRepository.findById(sellerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Seller", "id", sellerId));
            deal.setSeller(seller);

            if (req.getDealType() == DealType.CATEGORY || req.getDealType() == DealType.ORDER) {
                throw new AccessDeniedException("Sellers can only create product-level or seller-store promotions.");
            }

            if (req.getDealType() == DealType.PRODUCT) {
                if (req.getProductIds() == null || req.getProductIds().isEmpty()) {
                    throw new IllegalArgumentException("Please select at least one product for this deal.");
                }

                List<Product> products = new ArrayList<>();
                for (Long pid : req.getProductIds()) {
                    Product product = productRepository.findById(pid)
                            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", pid));

                    // CRITICAL SECURITY: Verify the product belongs to this seller
                    if (product.getSeller() == null || !product.getSeller().getId().equals(sellerId)) {
                        throw new AccessDeniedException("Product #" + pid + " does not belong to your seller account.");
                    }
                    products.add(product);
                }
                deal.setProducts(products);
            }
        } else {
            // Admin path: can target any scope
            if (req.getSellerId() != null) {
                Seller seller = sellerRepository.findById(req.getSellerId())
                        .orElseThrow(() -> new ResourceNotFoundException("Seller", "id", req.getSellerId()));
                deal.setSeller(seller);
            }

            if (req.getDealType() == DealType.PRODUCT && req.getProductIds() != null && !req.getProductIds().isEmpty()) {
                List<Product> products = productRepository.findAllById(req.getProductIds());
                deal.setProducts(products);
            }

            if (req.getDealType() == DealType.CATEGORY) {
                String slug = req.getCategorySlug();
                if (slug != null && !slug.isBlank()) {
                    deal.setCategorySlug(slug.trim());
                    homeCategoryRepository.findByCategoryId(slug.trim()).ifPresent(deal::setCategory);
                } else if (req.getCategoryId() != null) {
                    homeCategoryRepository.findById(req.getCategoryId()).ifPresent(hc -> {
                        deal.setCategory(hc);
                        deal.setCategorySlug(hc.getCategoryId());
                    });
                }
            }
        }

        Deal saved = dealRepository.save(deal);
        log.info("Deal created successfully: id={} title={} type={} seller={}",
                saved.getId(), saved.getTitle(), saved.getDealType(),
                saved.getSeller() != null ? saved.getSeller().getId() : "MARKETPLACE");

        if (saved.getSeller() != null) {
            notificationService.notifySeller(
                saved.getSeller(),
                NotificationType.DEAL_CREATED_SELLER,
                "Deal Created",
                "Your promotional deal \"" + saved.getTitle() + "\" is now active.",
                String.valueOf(saved.getId()),
                "DEAL",
                "/seller/deals"
            );
        } else {
            notificationService.broadcastToAdmins(
                NotificationType.SYSTEM_ALERT,
                "Marketplace Deal Created",
                "Marketplace deal \"" + saved.getTitle() + "\" is now live.",
                String.valueOf(saved.getId()),
                "DEAL",
                "/admin/deals"
            );
        }

        return DealResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public DealResponse updateDealFromRequest(Long id, DealRequest req, Long sellerId, boolean isAdmin) {
        Deal deal = dealRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deal", "id", id));

        // SECURITY: Verify authorization
        if (!isAdmin) {
            if (deal.getSeller() == null || !deal.getSeller().getId().equals(sellerId)) {
                throw new AccessDeniedException("You cannot edit a deal that does not belong to your account.");
            }
        }

        if (req.getTitle() != null && !req.getTitle().isBlank()) {
            deal.setTitle(req.getTitle().trim());
        }
        if (req.getDescription() != null) {
            deal.setDescription(req.getDescription());
        }
        if (req.getDiscountType() != null) {
            deal.setDiscountType(req.getDiscountType());
        }
        if (req.getDiscountValue() != null && req.getDiscountValue().compareTo(BigDecimal.ZERO) > 0) {
            if (deal.getDiscountType() == DiscountType.PERCENTAGE && req.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Percentage discount cannot exceed 100%.");
            }
            deal.setDiscountValue(req.getDiscountValue());
        }
        if (req.getMaxDiscountAmount() != null) {
            deal.setMaxDiscountAmount(req.getMaxDiscountAmount());
        }
        if (req.getMinOrderAmount() != null) {
            deal.setMinOrderAmount(req.getMinOrderAmount());
        }
        if (req.getStartAt() != null) {
            deal.setStartAt(req.getStartAt());
        }
        if (req.getEndAt() != null) {
            deal.setEndAt(req.getEndAt());
        }
        if (deal.getEndAt().isBefore(deal.getStartAt())) {
            throw new IllegalArgumentException("End date must be after start date.");
        }
        if (req.getActive() != null) {
            deal.setActive(req.getActive());
        }
        if (req.getUsageLimit() != null) {
            deal.setUsageLimit(req.getUsageLimit());
        }

        // Update products if specified
        if (req.getProductIds() != null) {
            if (!isAdmin) {
                List<Product> products = new ArrayList<>();
                for (Long pid : req.getProductIds()) {
                    Product product = productRepository.findById(pid)
                            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", pid));
                    if (product.getSeller() == null || !product.getSeller().getId().equals(sellerId)) {
                        throw new AccessDeniedException("Product #" + pid + " does not belong to your seller account.");
                    }
                    products.add(product);
                }
                deal.setProducts(products);
            } else {
                deal.setProducts(productRepository.findAllById(req.getProductIds()));
            }
        }

        Deal saved = dealRepository.save(deal);
        log.info("Deal updated: id={} title={}", saved.getId(), saved.getTitle());
        return DealResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void deleteDealWithAuth(Long id, Long sellerId, boolean isAdmin) {
        Deal deal = dealRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deal", "id", id));

        // SECURITY: Verify authorization
        if (!isAdmin) {
            if (deal.getSeller() == null || !deal.getSeller().getId().equals(sellerId)) {
                throw new AccessDeniedException("You cannot delete a deal that does not belong to your account.");
            }
        }

        dealRepository.delete(deal);
        log.info("Deal deleted: id={}", id);
    }

    @Override
    @Transactional
    public DealResponse toggleDealStatus(Long id, Long sellerId, boolean isAdmin) {
        Deal deal = dealRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deal", "id", id));

        // SECURITY: Verify authorization
        if (!isAdmin) {
            if (deal.getSeller() == null || !deal.getSeller().getId().equals(sellerId)) {
                throw new AccessDeniedException("You cannot toggle a deal that does not belong to your account.");
            }
        }

        deal.setActive(!deal.isActive());
        Deal saved = dealRepository.save(deal);
        log.info("Deal status toggled: id={} active={}", saved.getId(), saved.isActive());
        return DealResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DealResponse> getSellerDeals(Long sellerId) {
        if (sellerId == null) {
            return List.of();
        }
        return dealRepository.findBySellerIdOrderByCreatedAtDesc(sellerId)
                .stream()
                .map(DealResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DealResponse> getAllDealsAdmin() {
        return dealRepository.findAll()
                .stream()
                .map(DealResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DealResponse getDealById(Long id, Long sellerId, boolean isAdmin) {
        Deal deal = dealRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deal", "id", id));

        if (!isAdmin && (deal.getSeller() == null || !deal.getSeller().getId().equals(sellerId))) {
            throw new AccessDeniedException("Access denied to this deal.");
        }

        return DealResponse.fromEntity(deal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DealResponse> getActivePublicDeals() {
        return dealRepository.findAllCurrentlyActive(LocalDateTime.now())
                .stream()
                .map(DealResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
