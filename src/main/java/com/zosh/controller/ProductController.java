package com.zosh.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zosh.model.Product;
import com.zosh.service.ProductService;

@RestController
@RequestMapping("/products")
public class ProductController {

	@Autowired
	private ProductService productservice;
	
	@GetMapping("/{productId}")
	public ResponseEntity<Product> getProductById(@PathVariable Long productId){
		
		Product product = productservice.findProductById(productId);
		
		return new ResponseEntity<>(product,HttpStatus.OK);
	}
	
	@GetMapping("/search")
	public ResponseEntity<List<Product>> searchProduct(@RequestParam(required = false) String query){
		
		List<Product> products = productservice.searchProducts(query);
		
		return new ResponseEntity<>(products,HttpStatus.OK);
	}
	
	
	@GetMapping
	public ResponseEntity<Page<Product>> getAllProducts(
	        @RequestParam(required = false) String category,
	        @RequestParam(required = false) String brand,
	        @RequestParam(required = false) String colors,
	        @RequestParam(required = false) String sizes,
	        @RequestParam(required = false) Integer minPrice,
	        @RequestParam(required = false) Integer maxPrice,
	        @RequestParam(required = false) Integer minDiscount,
	        @RequestParam(required = false) String sort,
	        @RequestParam(required = false) String stock,
	        @RequestParam(defaultValue = "0") Integer pageNumber
	) {

	    Page<Product> products = productservice.getAllProducts(
	            category,
	            brand,
	            colors,
	            sizes,
	            minPrice,
	            maxPrice,
	            minDiscount,
	            sort,
	            stock,
	            pageNumber
	    );

	    return ResponseEntity.ok(products);
	}	
}
