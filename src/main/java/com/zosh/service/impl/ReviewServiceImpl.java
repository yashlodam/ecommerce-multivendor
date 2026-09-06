package com.zosh.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zosh.exceptions.ResourceNotFoundException;
import com.zosh.model.Product;
import com.zosh.model.Review;
import com.zosh.model.User;
import com.zosh.repository.OrderItemRepository;
import com.zosh.repository.ProductRepository;
import com.zosh.repository.ReviewRepository;
import com.zosh.request.CreateReviewRequest;
import com.zosh.service.ReviewService;

@Service
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override
    @Transactional
    public Review createReview(CreateReviewRequest req, User user, Product product) {
        // Business Rule: Customer must have purchased the product to write a review
        boolean hasPurchased = orderItemRepository.existsByUserIdAndProductId(user.getId(), product.getId());
        if (!hasPurchased) {
            throw new IllegalArgumentException("You can only review products that you have purchased.");
        }

        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setReviewText(req.getReviewText());
        review.setRating(req.getReviewRating());
        review.setProductImages(req.getProductImages());

        Review savedReview = reviewRepository.save(review);

        // Update product rating count
        product.setNumRatings(product.getNumRatings() + 1);
        productRepository.save(product);

        return savedReview;
    }

    @Override
    public List<Review> getReviewByProductId(Long productId) {
        return reviewRepository.findByProductId(productId);
    }

    @Override
    @Transactional
    public Review updateReview(Long reviewId, String reviewText, double rating, Long userId) {
        Review review = getReviewById(reviewId);

        // IDOR Authorization check
        if (!review.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to update this review.");
        }

        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }

        review.setReviewText(reviewText);
        review.setRating(rating);

        return reviewRepository.save(review);
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        Review r = getReviewById(reviewId);

        // IDOR Authorization check
        if (!r.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to delete this review.");
        }

        Product product = r.getProduct();
        if (product != null && product.getNumRatings() > 0) {
            product.setNumRatings(product.getNumRatings() - 1);
            productRepository.save(product);
        }

        reviewRepository.delete(r);
    }

    @Override
    public Review getReviewById(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));
    }
}
