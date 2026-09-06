package com.zosh.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zosh.model.Product;
import com.zosh.model.User;
import com.zosh.model.Wishlist;
import com.zosh.repository.WishlistRepository;
import com.zosh.service.WishlistService;

@Service
public class WishlistServiceImpl implements WishlistService {

    @Autowired
    private WishlistRepository wishlistRepo;

    @Override
    @Transactional
    public Wishlist createWishlist(User user) {
        Wishlist w = new Wishlist();
        w.setUser(user);
        return wishlistRepo.save(w);
    }

    @Override
    @Transactional
    public Wishlist getWishlistByUserId(User user) {
        Wishlist w = wishlistRepo.findByUserId(user.getId());
        if (w == null) {
            w = createWishlist(user);
        }
        return w;
    }

    @Override
    @Transactional
    public Wishlist addProductToWishlist(User user, Product product) {
        Wishlist wishlist = getWishlistByUserId(user);

        if (wishlist == null) {
            wishlist = createWishlist(user);
        }

        boolean exists = wishlist.getProducts().stream()
                .anyMatch(p -> p.getId() != null && p.getId().equals(product.getId()));

        if (exists) {
            wishlist.getProducts().removeIf(p -> p.getId() != null && p.getId().equals(product.getId()));
        } else {
            wishlist.getProducts().add(product);
        }

        return wishlistRepo.save(wishlist);
    }

    @Override
    @Transactional
    public Wishlist removeProductFromWishlist(User user, Long productId) {
        Wishlist wishlist = getWishlistByUserId(user);
        if (wishlist != null) {
            wishlist.getProducts().removeIf(p -> p.getId() != null && p.getId().equals(productId));
            return wishlistRepo.save(wishlist);
        }
        return createWishlist(user);
    }
}
