package com.zosh.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

import com.zosh.config.JwtProvider;
import com.zosh.domain.USER_ROLE;
import com.zosh.model.RefreshToken;
import com.zosh.model.User;
import com.zosh.repository.SellerRepository;
import com.zosh.repository.UserRepository;
import com.zosh.response.ApiResponse;
import com.zosh.response.AuthResponse;
import com.zosh.response.LoginRequest;
import com.zosh.service.AuthService;
import com.zosh.service.CookieService;
import com.zosh.service.RefreshTokenService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Spy
    private CookieService cookieService = new CookieService();

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SellerRepository sellerRepository;

    @InjectMocks
    private AuthController authController;

    private void initCookieService() {
        ReflectionTestUtils.setField(cookieService, "refreshTokenExpirationDays", 7);
        ReflectionTestUtils.setField(cookieService, "cookieSecure", false);
        ReflectionTestUtils.setField(cookieService, "cookieSameSite", "Lax");
    }

    @Test
    @DisplayName("login should return access token and set HttpOnly refresh token cookie")
    void signin_setsCookieAndReturnsJwt() {
        initCookieService();

        LoginRequest req = new LoginRequest();
        req.setEmail("user@example.com");
        req.setOtp("123456");

        AuthResponse authRes = new AuthResponse("access-jwt-123", "Login successful", USER_ROLE.ROLE_CUSTOMER);
        when(authService.siging(any(LoginRequest.class))).thenReturn(authRes);

        RefreshToken mockRt = new RefreshToken("rt-abc-456", "user@example.com", Instant.now().plusSeconds(600000));
        when(refreshTokenService.createRefreshToken("user@example.com")).thenReturn(mockRt);

        MockHttpServletResponse httpResponse = new MockHttpServletResponse();
        ResponseEntity<AuthResponse> response = authController.signinHandler(req, httpResponse);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("access-jwt-123", response.getBody().getJwt());
        assertNull(response.getBody().getRefreshToken(), "Refresh token must never be exposed in JSON body");

        String setCookie = httpResponse.getHeader("Set-Cookie");
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("refreshToken=rt-abc-456"));
        assertTrue(setCookie.contains("HttpOnly"));
    }

    @Test
    @DisplayName("refresh should rotate token, set new cookie, and return new short-lived access token")
    void refresh_success() {
        initCookieService();

        String oldCookie = "old-rt-789";
        RefreshToken rotatedToken = new RefreshToken("new-rt-999", "user@example.com", Instant.now().plusSeconds(600000));
        when(refreshTokenService.rotateRefreshToken(oldCookie)).thenReturn(rotatedToken);

        User user = new User();
        user.setEmail("user@example.com");
        user.setRole(USER_ROLE.ROLE_CUSTOMER);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        when(jwtProvider.generateToken("user@example.com", "ROLE_CUSTOMER")).thenReturn("new-access-jwt-999");

        MockHttpServletResponse httpResponse = new MockHttpServletResponse();
        ResponseEntity<AuthResponse> response = authController.refreshTokenHandler(oldCookie, httpResponse);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("new-access-jwt-999", response.getBody().getJwt());
        assertEquals(USER_ROLE.ROLE_CUSTOMER, response.getBody().getRole());
        assertNull(response.getBody().getRefreshToken(), "Refresh token must never be exposed in JSON body");

        String setCookie = httpResponse.getHeader("Set-Cookie");
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("refreshToken=new-rt-999"));
        assertTrue(setCookie.contains("HttpOnly"));
    }

    @Test
    @DisplayName("refresh without cookie should return 401 Unauthorized")
    void refresh_missingCookie() {
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();
        ResponseEntity<AuthResponse> response = authController.refreshTokenHandler(null, httpResponse);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody().getJwt());
    }

    @Test
    @DisplayName("refresh with invalid/expired token should return 401 and clear cookie")
    void refresh_invalidToken() {
        initCookieService();

        when(refreshTokenService.rotateRefreshToken("bad-token"))
                .thenThrow(new BadCredentialsException("Refresh token has expired. Please login again."));

        MockHttpServletResponse httpResponse = new MockHttpServletResponse();
        ResponseEntity<AuthResponse> response = authController.refreshTokenHandler("bad-token", httpResponse);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        String setCookie = httpResponse.getHeader("Set-Cookie");
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("Max-Age=0") || setCookie.contains("max-age=0"));
    }

    @Test
    @DisplayName("logout should revoke token and clear cookie")
    void logout_success() {
        initCookieService();

        MockHttpServletResponse httpResponse = new MockHttpServletResponse();
        ResponseEntity<ApiResponse> response = authController.logoutHandler("current-rt", httpResponse);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(refreshTokenService, times(1)).revokeToken("current-rt");

        String setCookie = httpResponse.getHeader("Set-Cookie");
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("Max-Age=0") || setCookie.contains("max-age=0"));
    }

    @Test
    @DisplayName("sendOtpHandler should invoke authService and return success ApiResponse")
    void sendOtp_success() {
        com.zosh.response.LoginOtpRequest req = new com.zosh.response.LoginOtpRequest();
        req.setEmail("user@example.com");
        req.setRole(USER_ROLE.ROLE_CUSTOMER);

        when(authService.sentLoginOtp("user@example.com", USER_ROLE.ROLE_CUSTOMER)).thenReturn("123456");

        ResponseEntity<ApiResponse> response = authController.sendOtpHandler(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("OTP sent successfully", response.getBody().getMessage());
        verify(authService, times(1)).sentLoginOtp("user@example.com", USER_ROLE.ROLE_CUSTOMER);
    }
}
