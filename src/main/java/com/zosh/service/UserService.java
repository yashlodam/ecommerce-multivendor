package com.zosh.service;

import com.zosh.model.User;

public interface UserService {

      User findUserByJwtToken(String jwt);
	
	 User findUserByEmail(String email);
}
