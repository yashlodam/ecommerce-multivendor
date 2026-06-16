package com.zosh.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zosh.model.Cart;
import com.zosh.model.CartItem;
import com.zosh.model.Product;
import com.zosh.model.User;
import com.zosh.request.AddItemRequest;
import com.zosh.response.ApiResponse;
import com.zosh.service.CartItemService;
import com.zosh.service.CartService;
import com.zosh.service.ProductService;
import com.zosh.service.UserService;

@RestController
@RequestMapping("/api/cart")
public class CartController {

	@Autowired
	private CartService cartService;
	
	@Autowired
	private CartItemService cartItemService;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private ProductService prodcutService;
	
	
	@GetMapping
	public ResponseEntity<Cart> findUserCartHandler(
			@RequestHeader("Authorization") String jwt)
	{
		User user = userService.findUserByJwtToken(jwt);
		
		Cart cart = cartService.findUserCart(user);
		
		return new ResponseEntity<>(cart,HttpStatus.OK);
	}
	
	
	@PutMapping("/add")
	public ResponseEntity<CartItem> addItemToCart(@RequestBody AddItemRequest req,
			@RequestHeader("Authorization") String jwt
			){
		
		User user = userService.findUserByJwtToken(jwt);
		Product product = prodcutService.findProductById(req.getProductId());
		
		CartItem item = cartService.addCartItem(user, product, req.getSize(), req.getQuantity());
		
		ApiResponse res = new ApiResponse();
		res.setMessage("Item Added To Cart Successfully");
		
		return new ResponseEntity<>(item,HttpStatus.ACCEPTED);
		
		
	}
	
	@DeleteMapping("/item/{cartItemId}")
	public ResponseEntity<ApiResponse> deleteCartItemHandler(
			@PathVariable Long cartItemId,
			@RequestHeader("Authorization") String jwt
			)
	{
		
		User user = userService.findUserByJwtToken(jwt);
		cartItemService.removeCartItem(user.getId(), cartItemId);
		
		ApiResponse res  = new ApiResponse();
		res.setMessage("Item Remove From Cart");
		
		return new ResponseEntity<>(res,HttpStatus.ACCEPTED);
		
		
		
	}
	
	@PutMapping("/item/{cartItemId}")
	public ResponseEntity<CartItem> updateCartItemHandler(
			@PathVariable Long cartItemId,
			@RequestBody CartItem cartItem,
			@RequestHeader("Authorization") String jwt
			){
		
		User user = userService.findUserByJwtToken(jwt);
		
		CartItem updatedCartItem = null;
		
		if(cartItem.getQuantity()>0) {
			updatedCartItem = cartItemService.updateCartItem(user.getId(), cartItemId, cartItem);
		}
		
		
		return new ResponseEntity<>(updatedCartItem,HttpStatus.ACCEPTED);
		
		
		
	}
	
}
