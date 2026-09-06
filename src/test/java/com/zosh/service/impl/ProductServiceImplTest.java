package com.zosh.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import com.zosh.model.Category;
import com.zosh.model.Product;
import com.zosh.model.ProductVariant;
import com.zosh.model.Seller;
import com.zosh.repository.CartItemRepository;
import com.zosh.repository.CategoryRepository;
import com.zosh.repository.OrderItemRepository;
import com.zosh.repository.ProductRepository;
import com.zosh.repository.ProductVariantRepository;
import com.zosh.repository.WishlistRepository;
import com.zosh.request.CreateProductRequest;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock private ProductRepository productRepo;
    @Mock private CategoryRepository categoryRepo;
    @Mock private ProductVariantRepository variantRepo;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private WishlistRepository wishlistRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Seller seller1;
    private Seller seller2;
    private Product product1;

    @BeforeEach
    void setUp() {
        seller1 = new Seller();
        seller1.setId(1L);
        seller1.setSellerName("Seller One");

        seller2 = new Seller();
        seller2.setId(2L);
        seller2.setSellerName("Seller Two");

        product1 = new Product();
        product1.setId(10L);
        product1.setTitle("Mechanical Keyboard");
        product1.setSellingPrice(3000);
        product1.setMrpPrice(5000);
        product1.setQuantity(20);
        product1.setSeller(seller1);
    }

    @Test
    @DisplayName("createProduct — creates product with hierarchy and discount calculation")
    void createProduct_Success() {
        CreateProductRequest req = new CreateProductRequest();
        req.setTitle("Mechanical Keyboard");
        req.setDescription("RGB gaming keyboard");
        req.setMrpPrice(5000);
        req.setSellingPrice(3000);
        req.setQuantity(15);
        req.setCategory("electronics");
        req.setCategory2("accessories");
        req.setCategory3("keyboards");
        req.setImages(List.of("https://example.com/img.jpg"));

        Category cat3 = new Category();
        cat3.setCategoryId("keyboards");

        when(categoryRepo.findByCategoryId("electronics")).thenReturn(new Category());
        when(categoryRepo.findByCategoryId("accessories")).thenReturn(new Category());
        when(categoryRepo.findByCategoryId("keyboards")).thenReturn(cat3);
        when(productRepo.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(100L);
            return p;
        });
        // variantRepo.save() is called to create the default variant
        when(variantRepo.save(any(ProductVariant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // productRepo.findById() is called after save to return the fully-populated product
        when(productRepo.findById(100L)).thenAnswer(invocation -> {
            Product p = new Product();
            p.setId(100L);
            p.setTitle("Mechanical Keyboard");
            p.setDiscountPercent(40);
            p.setQuantity(15);
            p.setSeller(seller1);
            return Optional.of(p);
        });

        Product created = productService.createProduct(req, seller1);

        assertNotNull(created);
        assertEquals("Mechanical Keyboard", created.getTitle());
        assertEquals(40, created.getDiscountPercent()); // (5000-3000)/5000 = 40%
        assertEquals(15, created.getQuantity());
        assertEquals(seller1, created.getSeller());
    }

    @Test
    @DisplayName("updateProduct — allows owner seller to update product details")
    void updateProduct_Owner_Success() {
        when(productRepo.findById(10L)).thenReturn(Optional.of(product1));
        when(productRepo.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product updateReq = new Product();
        updateReq.setTitle("Updated Keyboard Title");
        updateReq.setSellingPrice(2500);
        updateReq.setMrpPrice(5000);
        updateReq.setQuantity(30);

        Product updated = productService.updateProduct(10L, updateReq, 1L);

        assertEquals("Updated Keyboard Title", updated.getTitle());
        assertEquals(2500, updated.getSellingPrice());
        assertEquals(30, updated.getQuantity());
        assertEquals(50, updated.getDiscountPercent());
    }

    @Test
    @DisplayName("updateProduct — blocks other seller from modifying product (Multi-vendor isolation)")
    void updateProduct_OtherSeller_ThrowsAccessDenied() {
        when(productRepo.findById(10L)).thenReturn(Optional.of(product1));

        Product updateReq = new Product();
        updateReq.setTitle("Hacked Title");

        assertThrows(AccessDeniedException.class, () ->
                productService.updateProduct(10L, updateReq, 2L)); // Seller 2 trying to update Seller 1's product
    }

    @Test
    @DisplayName("deleteProduct — blocks other seller from deleting product (Multi-vendor isolation)")
    void deleteProduct_OtherSeller_ThrowsAccessDenied() {
        when(productRepo.findById(10L)).thenReturn(Optional.of(product1));

        assertThrows(AccessDeniedException.class, () ->
                productService.deleteProduct(10L, 2L));
    }

    @Test
    @DisplayName("deleteProduct — allows owner seller to delete product when no orders exist")
    void deleteProduct_Owner_Success() {
        when(productRepo.findById(10L)).thenReturn(Optional.of(product1));
        when(orderItemRepository.existsByProductId(10L)).thenReturn(false);
        when(wishlistRepository.findByProductsContaining(product1)).thenReturn(List.of());

        assertDoesNotThrow(() -> productService.deleteProduct(10L, 1L));
        verify(cartItemRepository, times(1)).deleteByProductId(10L);
        verify(productRepo, times(1)).delete(product1);
    }

    @Test
    @DisplayName("deleteProduct — throws ProductException when product has existing customer orders")
    void deleteProduct_WithExistingOrders_ThrowsProductException() {
        when(productRepo.findById(10L)).thenReturn(Optional.of(product1));
        when(orderItemRepository.existsByProductId(10L)).thenReturn(true);

        assertThrows(com.zosh.exceptions.ProductException.class, () ->
                productService.deleteProduct(10L, 1L));
        verify(productRepo, never()).delete(any(Product.class));
    }

    @Test
    @DisplayName("getAllProducts — executes search query and returns page of matching products")
    void getAllProducts_SearchQuery_Success() {
        Page<Product> mockPage = new PageImpl<>(List.of(product1));
        when(productRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        Page<Product> result = productService.getAllProducts(
                "apple", null, null, null, null, null, null, null, "price_low", "in_stock", 0);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(product1, result.getContent().get(0));
    }

    @Test
    @DisplayName("getDistinctBrands — returns all distinct brands when no category or query")
    void getDistinctBrands_NoFilter_Success() {
        when(productRepo.findDistinctBrands()).thenReturn(List.of("Apple", "Samsung", "Yash"));

        List<String> brands = productService.getDistinctBrands(null, null);

        assertEquals(3, brands.size());
        assertEquals("Apple", brands.get(0));
        verify(productRepo, times(1)).findDistinctBrands();
    }

    @Test
    @DisplayName("getDistinctBrands — queries by category hierarchy when category is provided")
    void getDistinctBrands_WithCategory_Success() {
        Category cat = new Category();
        cat.setId(1L);
        cat.setCategoryId("men");
        when(categoryRepo.findAll()).thenReturn(List.of(cat));
        when(productRepo.findDistinctBrandsByCategoryIds(Set.of(1L))).thenReturn(List.of("Yash"));

        List<String> brands = productService.getDistinctBrands("men", "");

        assertEquals(1, brands.size());
        assertEquals("Yash", brands.get(0));
        verify(productRepo, times(1)).findDistinctBrandsByCategoryIds(Set.of(1L));
    }

    @Test
    @DisplayName("getDistinctBrands — queries by search keyword when query is provided")
    void getDistinctBrands_WithQuery_Success() {
        when(productRepo.findDistinctBrandsByQuery("shirt")).thenReturn(List.of("Yash"));

        List<String> brands = productService.getDistinctBrands(null, "shirt");

        assertEquals(1, brands.size());
        assertEquals("Yash", brands.get(0));
        verify(productRepo, times(1)).findDistinctBrandsByQuery("shirt");
    }
}
