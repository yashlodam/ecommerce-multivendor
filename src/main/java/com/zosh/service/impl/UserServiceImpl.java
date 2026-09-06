package com.zosh.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zosh.config.JwtProvider;
import com.zosh.exceptions.ResourceNotFoundException;
import com.zosh.model.Cart;
import com.zosh.model.User;
import com.zosh.model.Wishlist;
import com.zosh.repository.CartItemRepository;
import com.zosh.repository.CartRepository;
import com.zosh.repository.OrderRepository;
import com.zosh.repository.UserRepository;
import com.zosh.repository.WishlistRepository;
import com.zosh.service.UserService;

@Service
public class UserServiceImpl implements UserService{
	
	@Autowired
	private UserRepository userepo;
	
	@Autowired
	private JwtProvider jwtprovider;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private CartRepository cartRepository;

	@Autowired
	private CartItemRepository cartItemRepository;

	@Autowired
	private WishlistRepository wishlistRepository;

	@Autowired
	private com.zosh.repository.NotificationRepository notificationRepository;

	@Autowired
	private com.zosh.repository.VerificationCodeRepository verificationCodeRepository;

	@Override
	public User findUserByJwtToken(String jwt) {
		String email = jwtprovider.getEmailFromJwtToken(jwt);
		return userepo.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
	}

	@Override
	public User findUserByEmail(String email) {
		return userepo.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
	}

	@Override
	@Transactional
	public User banUser(Long userId) {
		User user = userepo.findById(userId)
	            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
	    user.setEnabled(false);
	    return userepo.save(user);
	}

	@Override
	public List<User> getAllUsers() {
		return userepo.findAll();
	}

	@Override
	public User findUserById(Long id) {
		return userepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
	}

	@Override
	@Transactional
	public User unbanUser(Long id) {
		User user = userepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
		user.setEnabled(true);
		return userepo.save(user);
	}

	@Override
	@Transactional
	public void deleteUser(Long id) {
		User user = userepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

		if (!orderRepository.findByUserId(id).isEmpty()) {
			throw new IllegalStateException(
				"Cannot delete user with existing orders as orders must be preserved for financial auditing. Please ban or disable the user instead.");
		}

		// Delete user's cart and cart items if present
		Cart cart = cartRepository.findByUserId(id);
		if (cart != null) {
			cartItemRepository.deleteByCart(cart);
			cartRepository.delete(cart);
		}

		// Delete user's wishlist if present
		Wishlist wishlist = wishlistRepository.findByUserId(id);
		if (wishlist != null) {
			wishlistRepository.delete(wishlist);
		}

		userepo.delete(user);
	}

	@Override
	@Transactional
	public void deleteCustomerAccount(User user) {
		if (user == null || user.getId() == null) {
			throw new ResourceNotFoundException("User", "id", "null");
		}
		Long userId = user.getId();

		// 1. Guard against deletion if active orders exist
		List<com.zosh.model.Order> orders = orderRepository.findByUserId(userId);
		boolean hasActiveOrders = orders.stream().anyMatch(o -> 
			o.getOrderStatus() == com.zosh.domain.OrderStatus.PENDING ||
			o.getOrderStatus() == com.zosh.domain.OrderStatus.PLACED ||
			o.getOrderStatus() == com.zosh.domain.OrderStatus.CONFIRMED ||
			o.getOrderStatus() == com.zosh.domain.OrderStatus.SHIPPED
		);
		if (hasActiveOrders) {
			throw new IllegalStateException(
				"Cannot delete account while you have active orders in progress. Please wait until your orders are delivered or cancel them first."
			);
		}

		// 2. Clear ephemeral and active customer data
		Cart cart = cartRepository.findByUserId(userId);
		if (cart != null) {
			cartItemRepository.deleteByCart(cart);
			cartRepository.delete(cart);
		}

		Wishlist wishlist = wishlistRepository.findByUserId(userId);
		if (wishlist != null) {
			wishlistRepository.delete(wishlist);
		}

		// Unlink notifications foreign key to prevent constraint violation
		try {
			notificationRepository.unlinkUserFromNotifications(userId);
		} catch (Exception e) {
			// ignore or log
		}

		// Remove verification codes
		try {
			com.zosh.model.VerificationCode code = verificationCodeRepository.findByEmail(user.getEmail());
			if (code != null) {
				verificationCodeRepository.delete(code);
			}
		} catch (Exception e) {
			// ignore
		}

		// Clear saved addresses from user's address book
		user.getAddresses().clear();

		// 3. If user has no orders, permanently purge the user record
		if (orders.isEmpty()) {
			userepo.delete(user);
		} else {
			// Preserves financial auditability for past delivered/cancelled orders,
			// while wiping all personal identifiable information and freeing their original email.
			user.setEnabled(false);
			user.setFullName("Deleted Customer");
			user.setMobile(null);
			user.setPassword("ACCOUNT_DELETED_" + System.currentTimeMillis());
			user.setEmail("deleted_" + userId + "_" + System.currentTimeMillis() + "@deleted.shopsphere.com");
			userepo.save(user);
		}
	}
}
