package com.zosh.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.zosh.config.JwtProvider;
import com.zosh.model.User;
import com.zosh.repository.UserRepository;
import com.zosh.service.UserService;

@Service
public class UserServiceImpl implements UserService{
	
	@Autowired
	private UserRepository userepo;
	
	@Autowired
	private JwtProvider jwtprovider;

	@Override
	public User findUserByJwtToken(String jwt) {
		
		String email = jwtprovider.getEmailFromJwtToken(jwt);
		
		User user = userepo.findByEmail(email)
				.orElseThrow(()-> new UsernameNotFoundException("user not found with email"));
		
		return user;
	}

	@Override
	public User findUserByEmail(String email) {
		
		User user = userepo.findByEmail(email)
				.orElseThrow(()-> new UsernameNotFoundException("user not found with email"));
		
		return user;
	}

	
	
}
