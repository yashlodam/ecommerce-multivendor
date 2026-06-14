package com.zosh.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.zosh.config.JwtProvider;
import com.zosh.domain.USER_ROLE;
import com.zosh.model.Cart;
import com.zosh.model.User;
import com.zosh.model.VerificationCode;
import com.zosh.repository.CartRepository;
import com.zosh.repository.UserRepository;
import com.zosh.repository.VerificationCodeRepository;
import com.zosh.response.AuthResponse;
import com.zosh.response.LoginRequest;
import com.zosh.response.SignupRequest;
import com.zosh.service.AuthService;
import com.zosh.service.EmailService;
import com.zosh.utils.OtpUtil;

@Service
public class AuthServiceImpl implements AuthService {
	
	@Autowired
	private UserRepository userepo;
	
	@Autowired
	private CartRepository cartrepo;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Autowired
	private JwtProvider jwtprovider;
	
	@Autowired
	private VerificationCodeRepository repo;
	
	@Autowired
	private EmailService emailService;
	
	@Autowired
	private CustomeUserServiceImpl customeUserServiceImpl;

	@Override
	public String createUser(SignupRequest req) {

	    VerificationCode verificationCode =
	            repo.findByEmail(req.getEmail());

	    if (verificationCode == null) {
	        throw new IllegalArgumentException(
	                "OTP not found for email: " + req.getEmail());
	    }

	    if (!verificationCode.getOtp().equals(req.getOtp())) {
	        throw new IllegalArgumentException("Invalid OTP");
	    }

	    if (userepo.existsByEmail(req.getEmail())) {
	        throw new RuntimeException("Email already exists");
	    }

	    User user = new User();

	    user.setEmail(req.getEmail());
	    user.setFullName(req.getFullName());
	    user.setRole(USER_ROLE.ROLE_CUSTOMER);
	    user.setMobile("9665774924");

	    // IMPORTANT: use password, not OTP
	    user.setPassword(
	            passwordEncoder.encode(passwordEncoder.encode(req.getOtp())));

	    User savedUser = userepo.save(user);

	    Cart cart = new Cart();
	    cart.setUser(savedUser);
	    cartrepo.save(cart);

	    // Delete OTP after successful verification
	    repo.delete(verificationCode);

	    List<GrantedAuthority> authorities =
	            new ArrayList<>();

	    authorities.add(
	            new SimpleGrantedAuthority(
	                    USER_ROLE.ROLE_CUSTOMER.name()));

	    Authentication authentication =
	            new UsernamePasswordAuthenticationToken(
	                    savedUser.getEmail(),
	                    null,
	                    authorities);

	    SecurityContextHolder.getContext()
	            .setAuthentication(authentication);

	    return jwtprovider.generateToken(authentication);
	}
	@Override
	public void sentLoginOtp(String email) {

	    String SIGNING_PREFIX = "signin_";

	    if (email.startsWith(SIGNING_PREFIX)) {

	        String actualEmail =
	                email.substring(SIGNING_PREFIX.length());

	        userepo.findByEmail(actualEmail)
	                .orElseThrow(() ->
	                        new UsernameNotFoundException(
	                                "User not found with email: "
	                                        + actualEmail));

	        email = actualEmail;
	    }

	    VerificationCode existingOtp =
	            repo.findByEmail(email);

	    if (existingOtp != null) {
	        repo.delete(existingOtp);
	    }

	    String otp = OtpUtil.generateOtp();

	    VerificationCode verificationCode =
	            new VerificationCode();

	    verificationCode.setEmail(email);
	    verificationCode.setOtp(otp);

	    repo.save(verificationCode);

	    emailService.sendVerificationOtpEmail(
	            email,
	            otp,
	            "Zosh Bazar Login/Signup OTP",
	            "Use the following OTP to verify your account."
	    );
	}
	@Override
	public AuthResponse siging(LoginRequest req) {

	    String username = req.getEmail();
	    String otp = req.getOtp();

	    Authentication authentication =
	            authenticate(username, otp);

	    SecurityContextHolder.getContext()
	            .setAuthentication(authentication);

	    String token =
	            jwtprovider.generateToken(authentication);

	    User user = userepo.findByEmail(username)
	            .orElseThrow(() ->
	                    new UsernameNotFoundException(
	                            "User not found"));

	    AuthResponse response = new AuthResponse();
	    response.setJwt(token);
	    response.setMessage("Login Success");
	    response.setRole(user.getRole());

	    return response;
	}
	private Authentication authenticate(
	        String username,
	        String otp) {

	    UserDetails userDetails =
	            customeUserServiceImpl.loadUserByUsername(username);

	    VerificationCode verificationCode =
	            repo.findByEmail(username);

	    if (verificationCode == null) {
	        throw new BadCredentialsException("OTP not found");
	    }

	    if (!verificationCode.getOtp().equals(otp)) {
	        throw new BadCredentialsException("Invalid OTP");
	    }

	    repo.delete(verificationCode);

	    return new UsernamePasswordAuthenticationToken(
	            userDetails,
	            null,
	            userDetails.getAuthorities());
	}
}

