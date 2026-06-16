package com.zosh.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zosh.exceptions.ProductException;
import com.zosh.model.Product;
import com.zosh.model.Seller;
import com.zosh.request.CreateProductRequest;
import com.zosh.service.ProductService;
import com.zosh.service.SellerService;

@RestController
@RequestMapping("/api/sellers/products")
public class SellerProductController {

	@Autowired
	private ProductService productservice;
	
	@Autowired
	private SellerService sellerService;
	
	@GetMapping
	public ResponseEntity<List<Product>> getProductBySellerId(
			@RequestHeader("Authorization") String jwt
			){
		
		Seller seller = sellerService.getSellerProfile(jwt);
		
		List<Product> products = productservice.getProductsBySellerId(seller.getId());
		
		return new ResponseEntity<>(products,HttpStatus.OK);
	}
	
	
	@PostMapping()
	public ResponseEntity<Product> createProduct(
			@RequestBody CreateProductRequest req,
			@RequestHeader("Authorization") String jwt
			){
		
		Seller seller = sellerService.getSellerProfile(jwt);
		
		Product product = productservice.createProduct(req, seller);
		
		return new ResponseEntity<>(product,HttpStatus.CREATED);
	}
	
	
	@DeleteMapping("/{productId}")
	public ResponseEntity<Void> deleteProduct(@PathVariable Long productId){
		
		try {
			productservice.deleteProduct(productId);
			return new ResponseEntity<>(HttpStatus.OK);
		} catch(ProductException e) {
			
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}
	
	
	@PutMapping("/{productId}")
	public ResponseEntity<Product> updateProduct(@PathVariable Long productId,@RequestBody Product product){
		
		try {
			
			Product updateProduct = productservice.updateProduct(productId, product);
			
			return new ResponseEntity<>(updateProduct,HttpStatus.OK);
		} catch(ProductException e) {
			
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}
	
	
	
	
	
}
