package com.zosh.service;

import java.util.List;

import com.zosh.model.User;

public interface UserService {

      User findUserByJwtToken(String jwt);
	
	 User findUserByEmail(String email);

	 User banUser(Long userId);
	 
	 List<User> getAllUsers();

	 User findUserById(Long id);


	 User unbanUser(Long id);

	 void deleteUser(Long id);
}
