package com.zosh.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import java.util.Map;

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zosh.config.JwtProvider;
import com.zosh.domain.USER_ROLE;
import com.zosh.model.RefreshToken;
import com.zosh.model.Seller;
import com.zosh.model.User;
import com.zosh.repository.SellerRepository;
import com.zosh.repository.UserRepository;
import com.zosh.response.ApiResponse;
import com.zosh.response.AuthResponse;
import com.zosh.response.LoginOtpRequest;
import com.zosh.response.LoginRequest;
import com.zosh.response.SignupRequest;
import com.zosh.service.AuthService;
import com.zosh.service.CookieService;
import com.zosh.service.RefreshTokenService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Dual-token authentication: Short-lived Access Token + HttpOnly Refresh Token Cookie")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final String SELLER_PREFIX = "seller_";

    @Autowired
    private AuthService authservice;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private CookieService cookieService;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SellerRepository sellerRepository;

    @PostMapping("/signup")
    @Operation(summary = "Register a new customer account using OTP, issuing short-lived access token and HttpOnly refresh cookie")
    public ResponseEntity<AuthResponse> createUserHandler(
            @Valid @RequestBody SignupRequest req,
            HttpServletResponse httpResponse) {

        String jwt = authservice.createUser(req);

        // Issue refresh token and attach HttpOnly cookie
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(req.getEmail());
        cookieService.attachRefreshTokenCookie(httpResponse, refreshToken.getToken());

        AuthResponse res = new AuthResponse(jwt, "Registered successfully", USER_ROLE.ROLE_CUSTOMER);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/sent/login-signup-otp")
    @Operation(summary = "Send an OTP code for customer/seller signup or login")
    public ResponseEntity<ApiResponse> sendOtpHandler(
            @Valid @RequestBody LoginOtpRequest req) {

        authservice.sentLoginOtp(req.getEmail(), req.getRole());

        ApiResponse res = new ApiResponse("OTP sent successfully");
        return ResponseEntity.ok(res);
    }

    @PostMapping("/login")
    @Operation(summary = "Login using email and OTP or password, issuing short-lived access token and HttpOnly refresh cookie")
    public ResponseEntity<AuthResponse> signinHandler(
            @Valid @RequestBody LoginRequest req,
            HttpServletResponse httpResponse) {

        AuthResponse response = authservice.siging(req);

        // Extract the actual email (stripping seller_ prefix if present)
        String actualEmail = req.getEmail().startsWith(SELLER_PREFIX)
                ? req.getEmail().substring(SELLER_PREFIX.length())
                : req.getEmail();

        // Issue refresh token and attach HttpOnly cookie
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(actualEmail);
        cookieService.attachRefreshTokenCookie(httpResponse, refreshToken.getToken());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Silently refresh the short-lived access token using HttpOnly cookie, JSON body, or header")
    public ResponseEntity<AuthResponse> refreshTokenHandler(
            @CookieValue(name = CookieService.REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshTokenCookie,
            @RequestBody(required = false) Map<String, String> requestBody,
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshTokenHeader,
            HttpServletResponse httpResponse) {

        String tokenToVerify = null;
        if (refreshTokenCookie != null && !refreshTokenCookie.isBlank()) {
            tokenToVerify = refreshTokenCookie.trim();
        } else if (requestBody != null && requestBody.containsKey("refreshToken") && requestBody.get("refreshToken") != null && !requestBody.get("refreshToken").isBlank()) {
            tokenToVerify = requestBody.get("refreshToken").trim();
        } else if (refreshTokenHeader != null && !refreshTokenHeader.isBlank()) {
            tokenToVerify = refreshTokenHeader.trim();
        }

        if (tokenToVerify == null || tokenToVerify.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(null, "Refresh token is missing. Please log in again.", null));
        }

        try {
            // Rotate the refresh token (validates expiry, invalidates old token, creates new token)
            RefreshToken rotatedToken = refreshTokenService.rotateRefreshToken(tokenToVerify);
            String email = rotatedToken.getEmail();

            // Determine user role
            USER_ROLE role = resolveRoleForEmail(email);

            // Generate new short-lived access token
            String newAccessToken = jwtProvider.generateToken(email, role.name());

            // Attach new rotated refresh token cookie
            cookieService.attachRefreshTokenCookie(httpResponse, rotatedToken.getToken());

            AuthResponse authResponse = new AuthResponse(newAccessToken, "Token refreshed successfully", role);

            return ResponseEntity.ok(authResponse);

        } catch (BadCredentialsException e) {
            log.warn("Refresh token validation failed: {}", e.getMessage());
            // Clear invalid cookie
            cookieService.clearRefreshTokenCookie(httpResponse);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(null, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error during token refresh: {}", e.getMessage(), e);
            cookieService.clearRefreshTokenCookie(httpResponse);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponse(null, "Could not refresh token", null));
        }
    }

    public ResponseEntity<AuthResponse> refreshTokenHandler(
            String refreshTokenCookie,
            HttpServletResponse httpResponse) {
        return refreshTokenHandler(refreshTokenCookie, null, null, httpResponse);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user: revokes the refresh token in database and destroys the HttpOnly cookie")
    public ResponseEntity<ApiResponse> logoutHandler(
            @CookieValue(name = CookieService.REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshTokenCookie,
            @RequestBody(required = false) Map<String, String> requestBody,
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshTokenHeader,
            HttpServletResponse httpResponse) {

        String tokenToRevoke = null;
        if (refreshTokenCookie != null && !refreshTokenCookie.isBlank()) {
            tokenToRevoke = refreshTokenCookie.trim();
        } else if (requestBody != null && requestBody.containsKey("refreshToken") && requestBody.get("refreshToken") != null && !requestBody.get("refreshToken").isBlank()) {
            tokenToRevoke = requestBody.get("refreshToken").trim();
        } else if (refreshTokenHeader != null && !refreshTokenHeader.isBlank()) {
            tokenToRevoke = refreshTokenHeader.trim();
        }

        if (tokenToRevoke != null && !tokenToRevoke.isBlank()) {
            refreshTokenService.revokeToken(tokenToRevoke);
        }

        // Clear the cookie in browser
        cookieService.clearRefreshTokenCookie(httpResponse);

        return ResponseEntity.ok(new ApiResponse("Logged out successfully"));
    }

    public ResponseEntity<ApiResponse> logoutHandler(
            String refreshTokenCookie,
            HttpServletResponse httpResponse) {
        return logoutHandler(refreshTokenCookie, null, null, httpResponse);
    }

    private USER_ROLE resolveRoleForEmail(String email) {
        Seller seller = sellerRepository.findByEmail(email);
        if (seller != null) {
            return seller.getRole() != null ? seller.getRole() : USER_ROLE.ROLE_SELLER;
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            return user.getRole() != null ? user.getRole() : USER_ROLE.ROLE_CUSTOMER;
        }

        return USER_ROLE.ROLE_CUSTOMER;
    }
}
