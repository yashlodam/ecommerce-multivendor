package com.zosh.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.transaction.annotation.Transactional;

import com.zosh.config.JwtProvider;
import com.zosh.domain.USER_ROLE;
import com.zosh.exceptions.DuplicateResourceException;
import com.zosh.exceptions.OtpExpiredException;
import com.zosh.exceptions.ResourceNotFoundException;
import com.zosh.model.Cart;
import com.zosh.model.Seller;
import com.zosh.model.User;
import com.zosh.model.VerificationCode;
import com.zosh.repository.CartRepository;
import com.zosh.repository.SellerRepository;
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

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final String SELLER_PREFIX = "seller_";

    @Autowired private UserRepository userRepo;
    @Autowired private CartRepository cartRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private VerificationCodeRepository verificationCodeRepo;
    @Autowired private EmailService emailService;
    @Autowired private SellerRepository sellerRepo;
    @Autowired private CustomeUserServiceImpl customUserService;

    @Value("${app.otp.expiry-minutes:10}")
    private int otpExpiryMinutes;

    /**
     * Registers a new customer after OTP verification.
     * Creates the user and an empty cart in a single transaction.
     */
    @Override
    @Transactional
    public String createUser(SignupRequest req) {

        VerificationCode verificationCode = verificationCodeRepo.findByEmail(req.getEmail());

        if (verificationCode == null) {
            throw new BadCredentialsException("No OTP found for email: " + req.getEmail()
                    + ". Please request an OTP first.");
        }

        if (verificationCode.isExpired()) {
            verificationCodeRepo.delete(verificationCode);
            throw new OtpExpiredException(req.getEmail());
        }

        if (!verificationCode.getOtp().equals(req.getOtp())) {
            throw new BadCredentialsException("Invalid OTP.");
        }

        if (userRepo.existsByEmail(req.getEmail())) {
            throw new DuplicateResourceException("An account with email '" + req.getEmail() + "' already exists.");
        }

        User user = new User();
        user.setEmail(req.getEmail());
        user.setFullName(req.getFullName());
        user.setRole(USER_ROLE.ROLE_CUSTOMER);
        // FIX: encode OTP once (not twice), used as a one-time password; user can update via profile
        user.setPassword(passwordEncoder.encode(req.getOtp()));

        User savedUser = userRepo.save(user);

        // Every customer gets an empty cart
        Cart cart = new Cart();
        cart.setUser(savedUser);
        cartRepo.save(cart);

        // Clean up OTP after successful use to prevent replay
        verificationCodeRepo.delete(verificationCode);

        log.info("New customer registered: {}", savedUser.getEmail());

        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority(USER_ROLE.ROLE_CUSTOMER.name()));

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                savedUser.getEmail(), null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        return jwtProvider.generateToken(authentication);
    }

    /**
     * Sends a login/signup OTP to the given email.
     * For login OTPs, verifies the account exists before sending.
     * The "signin_" prefix in email indicates a login attempt (vs signup).
     */
    @Override
    @Transactional
    public void sentLoginOtp(String email, USER_ROLE role) {

        // Determine if this is a login (existing account) or signup (new account) OTP
        boolean isLoginAttempt = email.startsWith("signin_");
        String actualEmail = isLoginAttempt ? email.substring("signin_".length()) : email;

        if (isLoginAttempt) {
            // Verify the account exists before sending OTP
            if (role == USER_ROLE.ROLE_SELLER) {
                Seller seller = sellerRepo.findByEmail(actualEmail);
                if (seller == null) {
                    throw new ResourceNotFoundException("Seller", "email", actualEmail);
                }
            } else {
                userRepo.findByEmail(actualEmail)
                        .orElseThrow(() -> new ResourceNotFoundException("User", "email", actualEmail));
            }
        }

        // Invalidate any existing OTP for this email
        VerificationCode existingOtp = verificationCodeRepo.findByEmail(actualEmail);
        if (existingOtp != null) {
            verificationCodeRepo.delete(existingOtp);
        }

        String otp = OtpUtil.generateOtp();

        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setEmail(actualEmail);
        verificationCode.setOtp(otp);
        verificationCode.setCreatedAt(LocalDateTime.now());
        verificationCode.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes));

        verificationCodeRepo.save(verificationCode);

        log.info("OTP sent to: {} (isLogin={})", actualEmail, isLoginAttempt);

        emailService.sendVerificationOtpEmail(
                actualEmail,
                otp,
                "ShopSphere Account Verification",
                "Use the One-Time Password (OTP) below to verify your ShopSphere account.");
    }

    /**
     * Authenticates a user/seller by verifying their OTP.
     * Returns a JWT token on success.
     */
    @Override
    @Transactional
    public AuthResponse siging(LoginRequest req) {

        Authentication authentication = authenticate(req.getEmail(), req.getOtp());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtProvider.generateToken(authentication);

        AuthResponse response = new AuthResponse();
        response.setJwt(token);
        response.setMessage("Login successful");

        if (req.getEmail().startsWith(SELLER_PREFIX)) {
            String email = req.getEmail().substring(SELLER_PREFIX.length());
            Seller seller = sellerRepo.findByEmail(email);
            if (seller == null) {
                throw new ResourceNotFoundException("Seller", "email", email);
            }
            response.setRole(seller.getRole());
        } else {
            User user = userRepo.findByEmail(req.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "email", req.getEmail()));

            if (!user.isEnabled()) {
                throw new BadCredentialsException("Your account has been suspended. Please contact support.");
            }
            response.setRole(user.getRole());
        }

        return response;
    }

    private Authentication authenticate(String username, String otp) {

        UserDetails userDetails = customUserService.loadUserByUsername(username);

        // Extract the actual email (strip seller prefix if present)
        String email = username.startsWith(SELLER_PREFIX)
                ? username.substring(SELLER_PREFIX.length())
                : username;

        VerificationCode verificationCode = verificationCodeRepo.findByEmail(email);

        if (verificationCode == null) {
            throw new BadCredentialsException("No OTP found. Please request a new OTP.");
        }

        if (verificationCode.isExpired()) {
            verificationCodeRepo.delete(verificationCode);
            throw new OtpExpiredException(email);
        }

        if (!verificationCode.getOtp().equals(otp)) {
            throw new BadCredentialsException("Invalid OTP.");
        }

        // Consume OTP — single use
        verificationCodeRepo.delete(verificationCode);

        return new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
    }
}
