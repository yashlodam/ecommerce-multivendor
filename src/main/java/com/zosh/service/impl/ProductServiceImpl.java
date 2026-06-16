package com.zosh.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.zosh.exceptions.ProductException;
import com.zosh.model.Category;
import com.zosh.model.Product;
import com.zosh.model.Seller;
import com.zosh.repository.CategoryRepository;
import com.zosh.repository.ProductRepository;
import com.zosh.request.CreateProductRequest;
import com.zosh.service.ProductService;

import jakarta.persistence.criteria.Join;
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
	
	@Override
	public Product createProduct(
	        CreateProductRequest req,
	        Seller seller) {

	    Category category1 = categoryrepo.findByCategoryId(req.getCategory());
	    
	    		if(category1 ==null) {
	    			Category category = new Category();
	    			category.setCategoryId(req.getCategory());
	    			category.setLevel(1);
	    			category1 = categoryrepo.save(category);
	    		}
	    Category category2 = categoryrepo.findByCategoryId(req.getCategory2());
	    
	    if(category2==null) {
	    	
	    	Category category = new Category();
			category.setCategoryId(req.getCategory2());
			category.setLevel(2);
			category.setParentCategory(category1);
			category2 = categoryrepo.save(category);
	    }
	    
	    Category category3 = categoryrepo.findByCategoryId(req.getCategory3());
	    		if(category3==null) {
	    	    	
	    	    	Category category = new Category();
	    			category.setCategoryId(req.getCategory3());
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
	    product.setSellingPrice(req.getSellingPrice());
	    product.setImages(req.getImages());
	    product.setMrpPrice(req.getMrpPrice());
	    product.setSizes(req.getSizes());
	    product.setDiscountPercent(discountPercentage);
	    
	    return productrepo.save(product);
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
		return product;
	}

	@Override
	public Page<Product> getAllProducts(
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

	    Specification<Product> spec = (root, query, cb) -> {

	        List<Predicate> predicates = new ArrayList<>();

	        // Category Filter
	        if (category != null && !category.isBlank()) {

	            Join<Product, Category> categoryJoin =
	                    root.join("category");

	            predicates.add(
	                    cb.equal(
	                            categoryJoin.get("categoryId"),
	                            category));
	        }

	        // Min Price Filter
	        if (minPrice != null) {
	            predicates.add(
	                    cb.greaterThanOrEqualTo(
	                            root.get("sellingPrice"),
	                            minPrice));
	        }

	        // Max Price Filter
	        if (maxPrice != null) {
	            predicates.add(
	                    cb.lessThanOrEqualTo(
	                            root.get("sellingPrice"),
	                            maxPrice));
	        }

	        // Color Filter (Case-insensitive + Partial Match)
	        if (colors != null && !colors.isBlank()) {
	            predicates.add(
	                    cb.like(
	                            cb.lower(root.get("color")),
	                            "%" + colors.toLowerCase() + "%"
	                    )
	            );
	        }

	        // Size Filter (Case-insensitive)
	        if (sizes != null && !sizes.isBlank()) {
	            predicates.add(
	                    cb.equal(
	                            cb.lower(root.get("sizes")),
	                            sizes.toLowerCase()
	                    )
	            );
	        }

	        // Discount Filter
	        if (minDiscount != null) {
	            predicates.add(
	                    cb.greaterThanOrEqualTo(
	                            root.get("discountPercent"),
	                            minDiscount));
	        }

	        return cb.and(predicates.toArray(new Predicate[0]));
	    };

	    Pageable pageable;

	    if (sort != null && !sort.isBlank()) {

	        switch (sort) {

	            case "price_low":
	                pageable = PageRequest.of(
	                        pageNumber != null ? pageNumber : 0,
	                        10,
	                        Sort.by("sellingPrice").ascending());
	                break;

	            case "price_high":
	                pageable = PageRequest.of(
	                        pageNumber != null ? pageNumber : 0,
	                        10,
	                        Sort.by("sellingPrice").descending());
	                break;

	            default:
	                pageable = PageRequest.of(
	                        pageNumber != null ? pageNumber : 0,
	                        10,
	                        Sort.unsorted());
	                break;
	        }

	    } else {

	        pageable = PageRequest.of(
	                pageNumber != null ? pageNumber : 0,
	                10,
	                Sort.unsorted());
	    }

	    return productrepo.findAll(spec, pageable);
	}
	@Override
	public List<Product> getProductsBySellerId(Long sellerId) {
		
		return productrepo.findBySellerId(sellerId);
	}

	@Override
	public List<Product> getFeaturedProducts() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Product> getRelatedProducts(Long productId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Product> searchProducts(String query) {
		// TODO Auto-generated method stub
		return productrepo.searchProduct(query);
	}

	@Override
	public List<Product> searchProductsByCategory(String category) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Product> searchProductsByBrand(String brand) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Product updateProduct(Long id, Product updatedProduct) {

	    Product existingProduct = findProductById(id);

	    existingProduct.setTitle(updatedProduct.getTitle());
	    existingProduct.setDescription(updatedProduct.getDescription());
	    existingProduct.setSellingPrice(updatedProduct.getSellingPrice());
	    existingProduct.setMrpPrice(updatedProduct.getMrpPrice());
	    existingProduct.setQuantity(updatedProduct.getQuantity());

	    return productrepo.save(existingProduct);
	}

	@Override
	public Product updateProductStatus(Long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Product updateProductStock(Long productId, Integer quantity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Product updateProductPrice(Long productId, Integer price) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Product updateProductDiscount(Long productId, Integer discountPercent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteProduct(Long id) {
		
		Product product = findProductById(id);
		
		productrepo.delete(product);
		
	}

	@Override
	public boolean isProductInStock(Long productId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Integer getAvailableQuantity(Long productId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getTotalProductsBySeller(Long sellerId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Product> getActiveProductsBySeller(Long sellerId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Product> getInactiveProductsBySeller(Long sellerId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Product> getPendingProducts() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Product> getApprovedProducts() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Product approveProduct(Long productId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Product rejectProduct(Long productId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getTotalProducts() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getTotalActiveProducts() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getOutOfStockProductsCount() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Product updateProduct(Long id, CreateProductRequest req) {
		// TODO Auto-generated method stub
		return null;
	}

}
