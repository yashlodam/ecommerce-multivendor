package com.zosh.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zosh.model.Product;
import com.zosh.model.Review;
import com.zosh.model.User;
import com.zosh.request.CreateReviewRequest;
import com.zosh.response.ApiResponse;
import com.zosh.service.ProductService;
import com.zosh.service.ReviewService;
import com.zosh.service.UserService;

@RestController
@RequestMapping("/api")
public class ReviewController {

	@Autowired
	private ReviewService reviewService;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private ProductService productService;
	
	@GetMapping("/products/{productId}/reviews")
	public ResponseEntity<List<Review>> getReviewsByProductId(
			@PathVariable Long productId
			)
	
	{
		List<Review> reviews = reviewService.getReviewByProductId(productId);
		
		return ResponseEntity.ok(reviews);
	}
	
	
	@PostMapping("/products/{productId}/reviews")
	public ResponseEntity<Review> writeReview(
			@RequestBody CreateReviewRequest req,
			@PathVariable Long productId,
			@RequestHeader("Authorization") String jwt
			)
	
	{
		
		User user = userService.findUserByJwtToken(jwt);
		
		Product product = productService.findProductById(productId);
		
		Review review = reviewService.createReview(req, user, product);
		
		return ResponseEntity.ok(review);
		
		
	}
	
	
	@PatchMapping("/reviews/{reviewId}")
	public ResponseEntity<Review> updateReview(
			@RequestBody CreateReviewRequest req,
			@PathVariable Long reviewId,
			@RequestHeader("Authorization") String jwt
			)
	{
		
		
		User user = userService.findUserByJwtToken(jwt);
		
		Review review = reviewService.updateReview(reviewId, req.getReviewText(), req.getReviewRating(), user.getId());
		
		return ResponseEntity.ok(review);
		
		
		
	}
	
	
	@DeleteMapping("/reviews/{reviewId}")
	public ResponseEntity<ApiResponse> deleteReview(
			@PathVariable Long reviewId,
			@RequestHeader("Authorization") String jwt
			)
	{
		
		User user = userService.findUserByJwtToken(jwt);
		
		reviewService.deleteReview(reviewId, user.getId());
		
		ApiResponse res = new ApiResponse();
		res.setMessage("Review deleted successfully");
		
		return ResponseEntity.ok(res);
		
		
		
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}

