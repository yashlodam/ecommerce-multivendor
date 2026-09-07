package com.zosh.config;

import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

/**
 * Provides JWT token generation and parsing.
 *
 * The signing key is loaded from the application property 'app.jwt.secret',
 * which must be sourced from the JWT_SECRET_KEY environment variable in production.
 */
@Component
public class JwtProvider {

    @Value("${app.jwt.secret:ShopSphereDefaultSecretKeyForTokenSigningInDevelopmentAndStagingOnlyMustBeLongEnough64Bytes}")
    private String secretKey;

    @Value("${app.jwt.access-token-expiration-ms:${app.jwt.expiration-ms:86400000}}")
    private long expirationMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        if (secretKey == null || secretKey.trim().length() < 32) {
            secretKey = JWT_CONSTANT.DEFAULT_SECRET_KEY;
        }
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    /**
     * Generates a signed JWT for the given authenticated principal.
     */
    public String generateToken(Authentication auth) {
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        String roles = populateAuthorities(authorities);

        return Jwts.builder()
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .claim("email", auth.getName())
                .claim("authorities", roles)
                .signWith(key)
                .compact();
    }

    /**
     * Generates a signed short-lived JWT for the given email and role string (e.g. during refresh).
     */
    public String generateToken(String email, String role) {
        String authorities = role != null ? role : "";

        return Jwts.builder()
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .claim("email", email)
                .claim("authorities", authorities)
                .signWith(key)
                .compact();
    }

    /**
     * Extracts the email claim from a JWT token string.
     * Strips the "Bearer " prefix if present.
     */
    public String getEmailFromJwtToken(String jwt) {
        if (jwt != null && jwt.startsWith("Bearer ")) {
            jwt = jwt.substring(7);
        }

        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(jwt)
                .getPayload();

        return claims.get("email", String.class);
    }

    /**
     * Returns the signing key (used by JwtTokenValidator).
     */
    public SecretKey getSigningKey() {
        return key;
    }

    private String populateAuthorities(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
    }
}