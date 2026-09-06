package com.zosh.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Service for securely generating and clearing HttpOnly authentication cookies.
 */
@Service
public class CookieService {

    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    @Value("${app.jwt.refresh-token-expiration-days:7}")
    private int refreshTokenExpirationDays;

    @Value("${app.jwt.cookie-secure:false}")
    private boolean cookieSecure;

    @Value("${app.jwt.cookie-same-site:Lax}")
    private String cookieSameSite;

    /**
     * Builds an HttpOnly, Secure, SameSite ResponseCookie containing the refresh token.
     */
    public ResponseCookie createRefreshTokenCookie(String token) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(Duration.ofDays(refreshTokenExpirationDays))
                .build();
    }

    /**
     * Builds an expired clearing cookie to remove the refresh token from the client browser.
     */
    public ResponseCookie createCleanRefreshTokenCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(0)
                .build();
    }

    /**
     * Attaches the refresh token cookie to the outgoing HTTP response.
     */
    public void attachRefreshTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = createRefreshTokenCookie(token);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * Clears the refresh token cookie on the outgoing HTTP response.
     */
    public void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = createCleanRefreshTokenCookie();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
