package com.zosh.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.zosh.exceptions.ResourceNotFoundException;
import com.zosh.model.Product;
import com.zosh.model.Review;
import com.zosh.model.User;
import com.zosh.repository.OrderItemRepository;
import com.zosh.repository.ProductRepository;
import com.zosh.repository.ReviewRepository;
import com.zosh.request.CreateReviewRequest;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock private ReviewRepository reviewRepo;
    @Mock private OrderItemRepository orderItemRepo;
    @Mock private ProductRepository productRepo;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User customer;
    private Product product;
    private CreateReviewRequest request;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setId(10L);
        customer.setFullName("Buyer One");

        product = new Product();
        product.setId(100L);
        product.setTitle("Premium Wireless Earbuds");
        product.setNumRatings(0);
        product.setReviews(new ArrayList<>());

        request = new CreateReviewRequest();
        request.setReviewText("Excellent sound quality!");
        request.setReviewRating(5.0);
        request.setProductImages(List.of("https://img.example.com/photo.jpg"));
    }

    @Test
    @DisplayName("createReview — successfully creates review when customer purchased product")
    void createReview_VerifiedPurchase_Success() {
        when(orderItemRepo.existsByUserIdAndProductId(10L, 100L)).thenReturn(true);
        when(reviewRepo.save(any(Review.class))).thenAnswer(invocation -> {
            Review r = invocation.getArgument(0);
            r.setId(1L);
            return r;
        });

        Review created = reviewService.createReview(request, customer, product);

        assertNotNull(created);
        assertEquals("Excellent sound quality!", created.getReviewText());
        assertEquals(5.0, created.getRating());
        assertEquals(1, product.getNumRatings());
        verify(productRepo, times(1)).save(product);
    }

    @Test
    @DisplayName("createReview — blocks review submission if customer did NOT purchase product")
    void createReview_UnverifiedPurchase_ThrowsException() {
        when(orderItemRepo.existsByUserIdAndProductId(10L, 100L)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                reviewService.createReview(request, customer, product));

        assertTrue(ex.getMessage().contains("only review products that you have purchased"));
        verify(reviewRepo, never()).save(any());
    }

    @Test
    @DisplayName("updateReview — allows author to update review")
    void updateReview_Author_Success() {
        Review existing = new Review();
        existing.setId(5L);
        existing.setUser(customer);
        existing.setReviewText("Old review");
        existing.setRating(3.0);

        when(reviewRepo.findById(5L)).thenReturn(Optional.of(existing));
        when(reviewRepo.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Review updated = reviewService.updateReview(5L, "Updated text", 4.5, 10L);

        assertEquals("Updated text", updated.getReviewText());
        assertEquals(4.5, updated.getRating());
    }

    @Test
    @DisplayName("updateReview — blocks non-author from updating review (IDOR protection)")
    void updateReview_NonAuthor_ThrowsAccessDenied() {
        Review existing = new Review();
        existing.setId(5L);
        existing.setUser(customer); // ID is 10

        when(reviewRepo.findById(5L)).thenReturn(Optional.of(existing));

        assertThrows(AccessDeniedException.class, () ->
                reviewService.updateReview(5L, "Hacked text", 1.0, 999L));
    }

    @Test
    @DisplayName("deleteReview — blocks non-author from deleting review (IDOR protection)")
    void deleteReview_NonAuthor_ThrowsAccessDenied() {
        Review existing = new Review();
        existing.setId(5L);
        existing.setUser(customer);

        when(reviewRepo.findById(5L)).thenReturn(Optional.of(existing));

        assertThrows(AccessDeniedException.class, () ->
                reviewService.deleteReview(5L, 999L));
    }
}
