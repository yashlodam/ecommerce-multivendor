package com.zosh.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zosh.exceptions.ProductException;
import com.zosh.model.Category;
import com.zosh.model.Product;
import com.zosh.model.ProductVariant;
import com.zosh.model.Seller;
import com.zosh.model.Wishlist;
import com.zosh.repository.CartItemRepository;
import com.zosh.repository.CategoryRepository;
import com.zosh.repository.OrderItemRepository;
import com.zosh.repository.ProductRepository;
import com.zosh.repository.ProductVariantRepository;
import com.zosh.repository.WishlistRepository;
import com.zosh.request.CreateProductRequest;
import com.zosh.request.ProductVariantRequest;
import com.zosh.service.ProductService;
import com.zosh.utils.OtpUtil;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;

@Service
public class ProductServiceImpl implements ProductService {

	@Autowired
	private ProductRepository productrepo;
	
	@Autowired
	private CategoryRepository categoryrepo;

	@Autowired
	private ProductVariantRepository variantRepo;

	@Autowired
	private OrderItemRepository orderItemRepository;

	@Autowired
	private CartItemRepository cartItemRepository;

	@Autowired
	private WishlistRepository wishlistRepository;

	@Autowired(required = false)
	@org.springframework.context.annotation.Lazy
	private com.zosh.service.PricingService pricingService;

	private void enrichProductWithDeals(Product product) {
		if (product == null || pricingService == null) return;
		try {
			com.zosh.dto.pricing.ProductPricingDto dto = pricingService.calculateProductPricing(product, null);
			product.setDealActive(dto.isDealActive());
			product.setEffectivePrice(dto.getEffectivePrice());
			product.setDiscountAmount(dto.getDiscountAmount());
			product.setAppliedDealTitle(dto.getAppliedDealTitle());
			product.setDealEndsAt(dto.getDealEndsAt());
			if (dto.isDealActive() && dto.getDiscountPercentage() > 0) {
				product.setDiscountPercent(dto.getDiscountPercentage());
			}
		} catch (Exception ignored) {}
	}

	private void enrichProductsWithDeals(List<Product> products) {
		if (products == null || pricingService == null) return;
		for (Product p : products) {
			enrichProductWithDeals(p);
		}
	}


	
	@Override
	@Transactional
	public Product createProduct(
	        CreateProductRequest req,
	        Seller seller) {

	    Category category1 = categoryrepo.findByCategoryId(req.getCategory());
	    
	    		if(category1 ==null) {
	    			Category category = new Category();
	    			category.setCategoryId(req.getCategory());
	    			category.setName(OtpUtil.formatCategoryName(req.getCategory()));
	    			category.setLevel(1);

	    			category1 = categoryrepo.save(category);;
	    			
	    		}
	    Category category2 = categoryrepo.findByCategoryId(req.getCategory2());
	    
	    if(category2==null) {
	    	
	    	Category category = new Category();
	    	category.setCategoryId(req.getCategory2());
	    	category.setName(OtpUtil.formatCategoryName(req.getCategory2()));
	    	category.setLevel(2);
	    	category.setParentCategory(category1);

	    	category2 = categoryrepo.save(category);
	    }
	    
	    Category category3 = categoryrepo.findByCategoryId(req.getCategory3());
	    		if(category3==null) {
	    	    	
	    			Category category = new Category();
	    			category.setCategoryId(req.getCategory3());
	    			category.setName(OtpUtil.formatCategoryName(req.getCategory3()));
	    			category.setLevel(3);
	    			category.setParentCategory(category2);

	    			category3 = categoryrepo.save(category);
	    	    }
	    
	    		int discountPercentage = calculateDiscountPercentage(req.getMrpPrice(),req.getSellingPrice());
	    		
	    Product product = new Product();
	    product.setSeller(seller);
	    product.setCategory(category3);
	    product.setDescription(req.getDescription());
	    product.setCreatedAt(LocalDateTime.now());
	    product.setTitle(req.getTitle());
	    product.setColor(req.getColor());
	    product.setBrand(req.getBrand());
	    product.setSellingPrice(req.getSellingPrice());
	    product.setImages(req.getImages());
	    product.setMrpPrice(req.getMrpPrice());
	    product.setSizes(req.getSizes());
	    product.setQuantity(req.getQuantity() > 0 ? req.getQuantity() : 1);
	    product.setDiscountPercent(discountPercentage);
	    
	    Product savedProduct = productrepo.save(product);

	    // ── Create variants ──────────────────────────────────────────
	    if (req.getVariants() != null && !req.getVariants().isEmpty()) {
	        // Seller supplied explicit variants
	        for (ProductVariantRequest vr : req.getVariants()) {
	            ProductVariant v = buildVariant(savedProduct, vr);
	            variantRepo.save(v);
	        }
	    } else {
	        // Legacy path: build one default variant from flat product fields
	        ProductVariant defaultVariant = new ProductVariant();
	        defaultVariant.setProduct(savedProduct);
	        defaultVariant.setVariantName(
	            req.getSizes() != null && !req.getSizes().isBlank()
	                ? req.getSizes().split(",")[0].trim()
	                : "Standard"
	        );
	        defaultVariant.setMrpPrice(req.getMrpPrice());
	        defaultVariant.setSellingPrice(req.getSellingPrice());
	        defaultVariant.setDiscountPercent(discountPercentage);
	        defaultVariant.setQuantity(req.getQuantity() > 0 ? req.getQuantity() : 1);
	        defaultVariant.setDefault(true);
	        variantRepo.save(defaultVariant);
	    }

	    return productrepo.findById(savedProduct.getId()).orElse(savedProduct);
	}
	
	private int calculateDiscountPercentage(int mrpPrice,int sellingPrice) {
		
		if(mrpPrice<=0) {
			throw new IllegalArgumentException("Actual Price must be greater than 0");
		}
		
		double discount = mrpPrice-sellingPrice;
		double discountpercentage = (discount/mrpPrice)*100;
		
		return (int) discountpercentage;
	}

	@Override
	public Product findProductById(Long id) {
		
		Product product = productrepo.findById(id)
				.orElseThrow(()-> new ProductException("product not found with given id"));
		enrichProductWithDeals(product);
		return product;
	}


	@Override
	public Page<Product> getAllProducts(
	        String query,
	        String category,
	        String brand,
	        String colors,
	        String sizes,
	        Integer minPrice,
	        Integer maxPrice,
	        Integer minDiscount,
	        String sort,
	        String stock,
	        Integer pageNumber) {

	    // Start with search specification
	    Specification<Product> spec = Specification.where(ProductSpecification.search(query));

	    // Category Filter (Smart Hierarchical & Suffix/Alias Matching)
	    if (category != null && !category.isBlank() && !"all".equalsIgnoreCase(category.trim())) {
	        Set<Long> categoryIds = resolveCategoryIds(category);
	        if (!categoryIds.isEmpty()) {
	            spec = spec.and((root, q, cb) -> {
	                Join<Product, Category> categoryJoin = root.join("category", JoinType.LEFT);
	                return categoryJoin.get("id").in(categoryIds);
	            });
	        } else {
	            String cleanCategory = category.trim().toLowerCase();
	            spec = spec.and((root, q, cb) -> {
	                Join<Product, Category> categoryJoin = root.join("category", JoinType.LEFT);
	                Join<Category, Category> parentJoin = categoryJoin.join("parentCategory", JoinType.LEFT);
	                Join<Category, Category> grandParentJoin = parentJoin.join("parentCategory", JoinType.LEFT);

	                Predicate currentCategory = cb.equal(cb.lower(categoryJoin.get("categoryId")), cleanCategory);
	                Predicate parentCategory = cb.equal(cb.lower(parentJoin.get("categoryId")), cleanCategory);
	                Predicate grandParentCategory = cb.equal(cb.lower(grandParentJoin.get("categoryId")), cleanCategory);

	                return cb.or(currentCategory, parentCategory, grandParentCategory);
	            });
	        }
	    }

	    // Brand Filter
	    if (brand != null && !brand.isBlank()) {

	        spec = spec.and((root, q, cb) ->
	                cb.like(
	                        cb.lower(root.get("brand")),
	                        "%" + brand.toLowerCase() + "%"
	                ));
	    }

	    // Color Filter
	    if (colors != null && !colors.isBlank()) {

	        spec = spec.and((root, q, cb) ->
	                cb.like(
	                        cb.lower(root.get("color")),
	                        "%" + colors.toLowerCase() + "%"
	                ));
	    }

	    // Size Filter
	    if (sizes != null && !sizes.isBlank()) {

	        spec = spec.and((root, q, cb) ->
	                cb.equal(
	                        cb.lower(root.get("sizes")),
	                        sizes.toLowerCase()
	                ));
	    }

	    // Min Price
	    if (minPrice != null) {

	        spec = spec.and((root, q, cb) ->
	                cb.greaterThanOrEqualTo(
	                        root.get("sellingPrice"),
	                        minPrice
	                ));
	    }

	    // Max Price
	    if (maxPrice != null) {

	        spec = spec.and((root, q, cb) ->
	                cb.lessThanOrEqualTo(
	                        root.get("sellingPrice"),
	                        maxPrice
	                ));
	    }

	    // Discount
	    if (minDiscount != null) {

	        spec = spec.and((root, q, cb) ->
	                cb.greaterThanOrEqualTo(
	                        root.get("discountPercent"),
	                        minDiscount
	                ));
	    }

	    // Stock Filter
	    if (stock != null && !stock.isBlank()) {

	        if (stock.equalsIgnoreCase("in_stock")) {

	            spec = spec.and((root, q, cb) ->
	                    cb.greaterThan(root.get("quantity"), 0));

	        } else if (stock.equalsIgnoreCase("out_of_stock")) {

	            spec = spec.and((root, q, cb) ->
	                    cb.equal(root.get("quantity"), 0));
	        }
	    }

	    Pageable pageable;

	    switch (sort == null ? "" : sort) {

	        case "price_low":
	            pageable = PageRequest.of(
	                    pageNumber,
	                    10,
	                    Sort.by("sellingPrice").ascending());
	            break;

	        case "price_high":
	            pageable = PageRequest.of(
	                    pageNumber,
	                    10,
	                    Sort.by("sellingPrice").descending());
	            break;

	        case "newest":
	            pageable = PageRequest.of(
	                    pageNumber,
	                    10,
	                    Sort.by("createdAt").descending());
	            break;

	        default:
	            pageable = PageRequest.of(
	                    pageNumber,
	                    10,
	                    Sort.by("id").descending());
	    }

	    Page<Product> page = productrepo.findAll(spec, pageable);
	    enrichProductsWithDeals(page.getContent());
	    return page;
	}
	@Override
	public List<Product> getProductsBySellerId(Long sellerId) {
		
		List<Product> list = productrepo.findBySellerId(sellerId);
		enrichProductsWithDeals(list);
		return list;
	}

	@Override
	public List<Product> getFeaturedProducts() {
		Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
		List<Product> list = productrepo.findAll(pageable).getContent();
		enrichProductsWithDeals(list);
		return list;
	}

	@Override
	public List<Product> getRelatedProducts(Long productId) {
		Product product = findProductById(productId);
		Specification<Product> spec = (root, query, cb) ->
			cb.and(
				cb.equal(root.get("category"), product.getCategory()),
				cb.notEqual(root.get("id"), productId)
			);
		Pageable pageable = PageRequest.of(0, 8, Sort.by("createdAt").descending());
		List<Product> list = productrepo.findAll(spec, pageable).getContent();
		enrichProductsWithDeals(list);
		return list;
	}

	@Override
	public List<Product> searchProducts(String query) {
	    Specification<Product> specification = ProductSpecification.search(query);
	    List<Product> list = productrepo.findAll(specification);
	    enrichProductsWithDeals(list);
	    return list;
	}


	@Override
	public List<Product> searchProductsByCategory(String category) {
		Specification<Product> spec = (root, query, cb) -> {
			Join<?, ?> categoryJoin = root.join("category");
			return cb.or(
				cb.equal(categoryJoin.get("categoryId"), category),
				cb.equal(categoryJoin.get("parentCategory").get("categoryId"), category)
			);
		};
		return productrepo.findAll(spec);
	}

	@Override
	public List<Product> searchProductsByBrand(String brand) {
		Specification<Product> spec = (root, query, cb) ->
			cb.like(cb.lower(root.get("title")), "%" + brand.toLowerCase() + "%");
		return productrepo.findAll(spec);
	}

	@Override
	public Product updateProduct(Long id, Product updatedProduct) {
	    return updateProduct(id, updatedProduct, null);
	}

	@Override
	public Product updateProduct(Long id, Product updatedProduct, Long sellerId) {
	    Product existingProduct = findProductById(id);

	    if (sellerId != null && !existingProduct.getSeller().getId().equals(sellerId)) {
	        throw new org.springframework.security.access.AccessDeniedException("You do not have permission to update this product.");
	    }

	    if (updatedProduct.getTitle() != null) existingProduct.setTitle(updatedProduct.getTitle());
	    if (updatedProduct.getDescription() != null) existingProduct.setDescription(updatedProduct.getDescription());
	    if (updatedProduct.getSellingPrice() != null) existingProduct.setSellingPrice(updatedProduct.getSellingPrice());
	    if (updatedProduct.getMrpPrice() != null) existingProduct.setMrpPrice(updatedProduct.getMrpPrice());
	    if (updatedProduct.getQuantity() != null) existingProduct.setQuantity(updatedProduct.getQuantity());
	    if (updatedProduct.getColor() != null) existingProduct.setColor(updatedProduct.getColor());
	    if (updatedProduct.getBrand() != null) existingProduct.setBrand(updatedProduct.getBrand());
	    if (updatedProduct.getSizes() != null) existingProduct.setSizes(updatedProduct.getSizes());
	    if (updatedProduct.getImages() != null && !updatedProduct.getImages().isEmpty()) existingProduct.setImages(updatedProduct.getImages());

	    if (existingProduct.getMrpPrice() != null && existingProduct.getSellingPrice() != null && existingProduct.getMrpPrice() > 0) {
	        existingProduct.setDiscountPercent(calculateDiscountPercentage(existingProduct.getMrpPrice(), existingProduct.getSellingPrice()));
	    }

	    return productrepo.save(existingProduct);
	}

	@Override
	public Product updateProductStatus(Long id) {
		return findProductById(id);
	}

	@Override
	public Product updateProductStock(Long productId, Integer quantity) {
		if (quantity == null || quantity < 0) {
			throw new IllegalArgumentException("Quantity cannot be negative");
		}
		Product product = findProductById(productId);
		product.setQuantity(quantity);
		return productrepo.save(product);
	}

	@Override
	public Product updateProductPrice(Long productId, Integer price) {
		if (price == null || price < 0) {
			throw new IllegalArgumentException("Price must be non-negative");
		}
		Product product = findProductById(productId);
		product.setSellingPrice(price);
		product.setDiscountPercent(calculateDiscountPercentage(product.getMrpPrice(), price));
		return productrepo.save(product);
	}

	@Override
	public Product updateProductDiscount(Long productId, Integer discountPercent) {
		if (discountPercent == null || discountPercent < 0 || discountPercent > 100) {
			throw new IllegalArgumentException("Discount percent must be between 0 and 100");
		}
		Product product = findProductById(productId);
		product.setDiscountPercent(discountPercent);
		int sellingPrice = product.getMrpPrice() - (product.getMrpPrice() * discountPercent / 100);
		product.setSellingPrice(sellingPrice);
		return productrepo.save(product);
	}

	@Override
	@Transactional
	public void deleteProduct(Long id) {
	    deleteProduct(id, null);
	}

	@Override
	@Transactional
	public void deleteProduct(Long id, Long sellerId) {
		Product product = findProductById(id);
		if (sellerId != null && !product.getSeller().getId().equals(sellerId)) {
		    throw new org.springframework.security.access.AccessDeniedException("You do not have permission to delete this product.");
		}

		if (orderItemRepository.existsByProductId(id)) {
		    throw new ProductException("Cannot delete product because it is associated with existing customer orders. Please set quantity to 0 instead to deactivate it.");
		}

		// Remove product from all customer carts
		cartItemRepository.deleteByProductId(id);

		// Remove product from any customer wishlists
		List<Wishlist> wishlists = wishlistRepository.findByProductsContaining(product);
		for (Wishlist w : wishlists) {
		    w.getProducts().remove(product);
		    wishlistRepository.save(w);
		}

		productrepo.delete(product);
	}

	@Override
	public boolean isProductInStock(Long productId) {
		Product product = findProductById(productId);
		return product.getQuantity() != null && product.getQuantity() > 0;
	}

	@Override
	public Integer getAvailableQuantity(Long productId) {
		Product product = findProductById(productId);
		return product.getQuantity() != null ? product.getQuantity() : 0;
	}

	@Override
	public Long getTotalProductsBySeller(Long sellerId) {
		return (long) productrepo.findBySellerId(sellerId).size();
	}

	@Override
	public List<Product> getActiveProductsBySeller(Long sellerId) {
		return productrepo.findBySellerId(sellerId).stream()
				.filter(p -> p.getQuantity() != null && p.getQuantity() > 0)
				.toList();
	}

	@Override
	public List<Product> getInactiveProductsBySeller(Long sellerId) {
		return productrepo.findBySellerId(sellerId).stream()
				.filter(p -> p.getQuantity() == null || p.getQuantity() == 0)
				.toList();
	}

	@Override
	public List<Product> getPendingProducts() {
		return productrepo.findAll();
	}

	@Override
	public List<Product> getApprovedProducts() {
		return productrepo.findAll();
	}

	@Override
	public Product approveProduct(Long productId) {
		return findProductById(productId);
	}

	@Override
	public Product rejectProduct(Long productId) {
		return findProductById(productId);
	}

	@Override
	public Long getTotalProducts() {
		return productrepo.count();
	}

	@Override
	public Long getTotalActiveProducts() {
		Specification<Product> spec = (root, query, cb) ->
			cb.greaterThan(root.get("quantity"), 0);
		return productrepo.count(spec);
	}

	@Override
	public Long getOutOfStockProductsCount() {
		Specification<Product> spec = (root, query, cb) ->
			cb.or(cb.isNull(root.get("quantity")), cb.equal(root.get("quantity"), 0));
		return productrepo.count(spec);
	}

	@Override
	public Product updateProduct(Long id, CreateProductRequest req) {
		Product existing = findProductById(id);
		if (req.getTitle() != null)       existing.setTitle(req.getTitle());
		if (req.getDescription() != null) existing.setDescription(req.getDescription());
		if (req.getMrpPrice() > 0)        existing.setMrpPrice(req.getMrpPrice());
		if (req.getSellingPrice() > 0) {
			existing.setSellingPrice(req.getSellingPrice());
			existing.setDiscountPercent(
				calculateDiscountPercentage(existing.getMrpPrice(), req.getSellingPrice()));
		}
		if (req.getColor() != null) existing.setColor(req.getColor());
		if (req.getSizes() != null) existing.setSizes(req.getSizes());
		if (req.getImages() != null && !req.getImages().isEmpty()) existing.setImages(req.getImages());
		return productrepo.save(existing);
	}

	// ─── Variant CRUD ─────────────────────────────────────────────────────────

	@Override
	public List<ProductVariant> getVariantsByProductId(Long productId) {
		return variantRepo.findByProductId(productId);
	}

	@Override
	@Transactional
	public ProductVariant createVariant(Long productId, ProductVariantRequest req, Long sellerId) {
		Product product = findProductById(productId);

		// IDOR check — seller can only add variants to their own products
		if (sellerId != null && !product.getSeller().getId().equals(sellerId)) {
			throw new AccessDeniedException("You do not have permission to modify this product.");
		}

		// Prevent duplicate variant names on the same product
		variantRepo.findByProductAndVariantName(product, req.getVariantName())
				   .ifPresent(v -> { throw new IllegalArgumentException(
				       "A variant named '" + req.getVariantName() + "' already exists for this product."); });

		ProductVariant variant = buildVariant(product, req);
		ProductVariant saved = variantRepo.save(variant);

		// Keep product-level flat quantity in sync (sum of variants)
		syncProductQuantity(product);

		return saved;
	}

	@Override
	@Transactional
	public ProductVariant updateVariant(Long variantId, ProductVariantRequest req, Long sellerId) {
		ProductVariant variant = variantRepo.findById(variantId)
				.orElseThrow(() -> new ProductException("Variant not found with id: " + variantId));

		// IDOR check
		if (sellerId != null && !variant.getProduct().getSeller().getId().equals(sellerId)) {
			throw new AccessDeniedException("You do not have permission to modify this variant.");
		}

		if (req.getVariantName() != null && !req.getVariantName().isBlank()) {
			variant.setVariantName(req.getVariantName());
		}
		if (req.getSku() != null) variant.setSku(req.getSku());
		if (req.getMrpPrice() > 0) variant.setMrpPrice(req.getMrpPrice());
		if (req.getSellingPrice() > 0) {
			variant.setSellingPrice(req.getSellingPrice());
			variant.setDiscountPercent(calculateDiscountPercentage(variant.getMrpPrice(), req.getSellingPrice()));
		}
		if (req.getQuantity() >= 0) variant.setQuantity(req.getQuantity());

		ProductVariant saved = variantRepo.save(variant);

		// Keep product-level flat quantity in sync
		syncProductQuantity(variant.getProduct());

		return saved;
	}

	@Override
	@Transactional
	public void deleteVariant(Long variantId, Long sellerId) {
		ProductVariant variant = variantRepo.findById(variantId)
				.orElseThrow(() -> new ProductException("Variant not found with id: " + variantId));

		// IDOR check
		if (sellerId != null && !variant.getProduct().getSeller().getId().equals(sellerId)) {
			throw new AccessDeniedException("You do not have permission to delete this variant.");
		}

		Product product = variant.getProduct();
		long count = variantRepo.countByProduct(product);
		if (count <= 1) {
			throw new IllegalStateException(
				"Cannot delete the only remaining variant. A product must have at least one variant.");
		}

		variantRepo.delete(variant);
		syncProductQuantity(product);
	}

	/**
	 * Builds a new ProductVariant entity from a request DTO.
	 * Does NOT save it — caller is responsible for persisting.
	 */
	private ProductVariant buildVariant(Product product, ProductVariantRequest req) {
		ProductVariant v = new ProductVariant();
		v.setProduct(product);
		v.setVariantName(req.getVariantName());
		v.setSku(req.getSku());
		v.setMrpPrice(req.getMrpPrice() > 0 ? req.getMrpPrice() : product.getMrpPrice());
		v.setSellingPrice(req.getSellingPrice() > 0 ? req.getSellingPrice() : product.getSellingPrice());
		v.setDiscountPercent(calculateDiscountPercentage(
			v.getMrpPrice(), v.getSellingPrice()
		));
		v.setQuantity(req.getQuantity());
		v.setDefault(req.isDefault());
		return v;
	}

	/**
	 * Keeps the flat Product.quantity field in sync with the sum of all variant quantities.
	 * This ensures backward-compat code that checks product.getQuantity() still works.
	 */
	private void syncProductQuantity(Product product) {
		int totalStock = variantRepo.findByProduct(product).stream()
				.mapToInt(v -> v.getQuantity() != null ? v.getQuantity() : 0)
				.sum();
		product.setQuantity(totalStock);
		productrepo.save(product);
	}

	@Override
	public List<String> getAllBrands() {
		return productrepo.findDistinctBrands();
	}

	@Override
	public List<String> getDistinctBrands(String category, String query) {
		boolean hasCategory = category != null && !category.isBlank() && !"all".equalsIgnoreCase(category.trim());
		boolean hasQuery = query != null && !query.isBlank();

		if (hasCategory) {
			Set<Long> categoryIds = resolveCategoryIds(category);
			if (!categoryIds.isEmpty()) {
				if (hasQuery) {
					return productrepo.findDistinctBrandsByCategoryIdsAndQuery(categoryIds, query.trim());
				} else {
					return productrepo.findDistinctBrandsByCategoryIds(categoryIds);
				}
			}
		}

		if (hasQuery) {
			return productrepo.findDistinctBrandsByQuery(query.trim());
		} else {
			return productrepo.findDistinctBrands();
		}
	}

	/**
	 * Resolves matching Category IDs including their entire subcategory hierarchy
	 * given an incoming category slug, keyword, or alias (e.g. "smartphones", "laptops", "men").
	 */
	private Set<Long> resolveCategoryIds(String categoryParam) {
		if (categoryParam == null || categoryParam.isBlank() || "all".equalsIgnoreCase(categoryParam.trim())) {
			return Collections.emptySet();
		}

		String clean = categoryParam.trim().toLowerCase().replace("-", "_");
		String cleanNoUnder = clean.replace("_", "");

		List<Category> allCategories = categoryrepo.findAll();
		Set<Long> matchedIds = new HashSet<>();

		for (Category c : allCategories) {
			if (c.getCategoryId() == null) continue;
			String cid = c.getCategoryId().toLowerCase();
			String cname = c.getName() != null ? c.getName().toLowerCase() : "";
			String cidNoUnder = cid.replace("_", "");

			// Distinct boundary for "men" to prevent falsely matching "women"
			if ("men".equals(clean)) {
				if ("men".equals(cid) || cid.startsWith("men_") || "men".equals(cname) || cname.startsWith("men ")) {
					matchedIds.add(c.getId());
				}
				continue;
			}

			if (cid.equals(clean)
					|| cid.endsWith("_" + clean)
					|| clean.equals(cid.replace("electronics_", "").replace("men_", "").replace("women_", ""))
					|| cidNoUnder.equals(cleanNoUnder)
					|| (clean.length() >= 4 && cid.contains(clean))
					|| (clean.length() >= 4 && cname.contains(clean))) {
				matchedIds.add(c.getId());
			}
		}

		// Recursively collect all descendant categories (children, grandchildren)
		boolean added = true;
		while (added) {
			added = false;
			for (Category c : allCategories) {
				if (c.getParentCategory() != null 
						&& matchedIds.contains(c.getParentCategory().getId()) 
						&& !matchedIds.contains(c.getId())) {
					matchedIds.add(c.getId());
					added = true;
				}
			}
		}

		return matchedIds;
	}
}
