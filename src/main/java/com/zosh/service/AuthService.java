package com.zosh.service;

import com.zosh.domain.USER_ROLE;
import com.zosh.model.User;
import com.zosh.response.AuthResponse;
import com.zosh.response.LoginRequest;
import com.zosh.response.SignupRequest;


public interface AuthService {

    String createUser(SignupRequest request);

    void sentLoginOtp(String email,USER_ROLE roleo);
    
    AuthResponse siging(LoginRequest req);

}