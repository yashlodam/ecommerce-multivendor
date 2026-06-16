package com.zosh.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zosh.model.Cart;
import com.zosh.model.CartItem;
import com.zosh.model.Product;
import com.zosh.model.User;
import com.zosh.repository.CartItemRepository;
import com.zosh.repository.CartRepository;
import com.zosh.service.CartService;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Override
    public CartItem addCartItem(
            User user,
            Product product,
            String size,
            int quantity) {

        Cart cart = findUserCart(user);

        CartItem existingItem =
                cartItemRepository.findByCartAndProductAndSize(
                        cart,
                        product,
                        size);

        // Product already exists in cart
        if (existingItem != null) {

            existingItem.setQuantity(
                    existingItem.getQuantity() + quantity);

            existingItem.setMrpPrice(
                    existingItem.getQuantity()
                    * product.getMrpPrice());

            existingItem.setSellingPrice(
                    existingItem.getQuantity()
                    * product.getSellingPrice());

            CartItem savedItem =
                    cartItemRepository.save(existingItem);

            findUserCart(user); // recalculate totals

            return savedItem;
        }

        // Create new Cart Item
        CartItem cartItem = new CartItem();

        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setUserId(user.getId());
        cartItem.setQuantity(quantity);
        cartItem.setSize(size);

        cartItem.setMrpPrice(
                quantity * product.getMrpPrice());

        cartItem.setSellingPrice(
                quantity * product.getSellingPrice());

        CartItem savedItem =
                cartItemRepository.save(cartItem);

        cart.getCartItems().add(savedItem);

        findUserCart(user); // recalculate totals

        return savedItem;
    }

    @Override
    public Cart findUserCart(User user) {

        Cart cart =
                cartRepository.findByUserId(user.getId());

        if (cart == null) {
            throw new RuntimeException("Cart not found");
        }

        int totalMrpPrice = 0;
        int totalSellingPrice = 0;
        int totalItems = 0;

        for (CartItem cartItem : cart.getCartItems()) {

            totalMrpPrice += cartItem.getMrpPrice();

            totalSellingPrice +=
                    cartItem.getSellingPrice();

            totalItems +=
                    cartItem.getQuantity();
        }

        cart.setTotalMrpPrice(totalMrpPrice);

        cart.setTotalSellingPrice(
                totalSellingPrice);

        cart.setTotalItem(totalItems);

        cart.setDiscount(
                calculateDiscountPercentage(
                        totalMrpPrice,
                        totalSellingPrice));

        cartRepository.save(cart);

        return cart;
    }

    private int calculateDiscountPercentage(
            int mrpPrice,
            int sellingPrice) {

        if (mrpPrice <= 0) {
            return 0;
        }

        double discount =
                mrpPrice - sellingPrice;

        double discountPercentage =
                (discount / mrpPrice) * 100;

        return (int) discountPercentage;
    }
}