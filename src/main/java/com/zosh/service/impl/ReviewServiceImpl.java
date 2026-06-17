package com.zosh.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zosh.model.Product;
import com.zosh.model.Review;
import com.zosh.model.User;
import com.zosh.repository.ReviewRepository;
import com.zosh.repository.UserRepository;
import com.zosh.request.CreateReviewRequest;
import com.zosh.service.ReviewService;

@Service
public class ReviewServiceImpl implements ReviewService {

	@Autowired
	private ReviewRepository reviewRepository;
	
	@Autowired
	private UserRepository userRepository;
	
	@Override
	public Review createReview(CreateReviewRequest req, User user, Product product) {
		
		Review review = new Review();
		review.setUser(user);
		review.setProduct(product);
		review.setReviewText(req.getReviewText());
		review.setRating(req.getReviewRating());
		review.setProductImages(req.getProductImages());
		
		product.getReviews().add(review);
		
		return reviewRepository.save(review);
	}

	@Override
	public List<Review> getReviewByProductId(Long productId) {
		
		return reviewRepository.findByProductId(productId);
	}

	@Override
	public Review updateReview(Long reviewId, String reviewText, double rating, Long userId) {

	    Review review = getReviewById(reviewId);

	    if (!review.getUser().getId().equals(userId)) {
	        throw new IllegalArgumentException("You can't update this review");
	    }

	    if (rating < 1 || rating > 5) {
	        throw new IllegalArgumentException("Rating must be between 1 and 5");
	    }

	    review.setReviewText(reviewText);
	    review.setRating(rating);

	    return reviewRepository.save(review);
	}

	@Override
	public void deleteReview(Long reviewId, Long userId) {
		
		Review r = getReviewById(reviewId);
		if(!r.getUser().getId().equals(userId)) {
			
			throw new IllegalArgumentException("you can't delete this review");
		}
		
		reviewRepository.delete(r);
		
		
		
	}

	@Override
	public Review getReviewById(Long reviewId) {
		
		Review r = reviewRepository.findById(reviewId).orElseThrow(()-> new IllegalArgumentException("Review not found with given id.."));
		
		return r;
	}

}
