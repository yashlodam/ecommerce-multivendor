package com.zosh.service;

import java.util.List;

import com.zosh.model.Product;
import com.zosh.model.Review;
import com.zosh.model.User;
import com.zosh.request.CreateReviewRequest;

public interface ReviewService {

	Review createReview(CreateReviewRequest req,User user,Product product);
	List<Review> getReviewByProductId(Long productId);
	
	Review updateReview(Long reviewId,String reviewText,double rating,Long userId);
	
	void deleteReview(Long reviewId,Long userId);
	
	Review getReviewById(Long reviewId);
}
