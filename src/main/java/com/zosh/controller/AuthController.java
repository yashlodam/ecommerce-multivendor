package com.zosh.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zosh.domain.USER_ROLE;
import com.zosh.model.User;
import com.zosh.model.VerificationCode;
import com.zosh.response.ApiResponse;
import com.zosh.response.AuthResponse;
import com.zosh.response.LoginRequest;
import com.zosh.response.SignupRequest;
import com.zosh.service.AuthService;

@RestController
@RequestMapping("/auth")
public class AuthController {
	
	@Autowired
	private AuthService authservice;

	@PostMapping("/signup")
	public ResponseEntity<AuthResponse> createUserHandler(@RequestBody SignupRequest req){
		
		String jwt = authservice.createUser(req);
		
		AuthResponse res = new AuthResponse();
		
		res.setJwt(jwt);
		res.setMessage("register sucess");
		res.setRole(USER_ROLE.ROLE_CUSTOMER);
		
		return ResponseEntity.ok(res);
		
	}
	 @PostMapping("/sent-login-otp")
	    public ResponseEntity<ApiResponse> sendOtpHandler(
	            @RequestBody VerificationCode req) {

	        authservice.sentLoginOtp(req.getEmail());

	        ApiResponse res = new ApiResponse();
	        res.setMessage("OTP sent successfully");

	        return ResponseEntity.ok(res);
	    }
	
	@PostMapping("/login")
    public ResponseEntity<AuthResponse> signinHandler(
            @RequestBody LoginRequest req) {

        AuthResponse response = authservice.siging(req);

        return ResponseEntity.ok(response);
    }
	 
}
