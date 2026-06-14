package com.zosh.config;

import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtProvider {

    private static final SecretKey key =
            Keys.hmacShaKeyFor(
                    JWT_CONSTANT.SECRET_KEY.getBytes());

    public static String generateToken(Authentication auth) {

        Collection<? extends GrantedAuthority> authorities =
                auth.getAuthorities();

        String roles = populateAuthorities(authorities);

        String jwt = Jwts.builder()
                .issuedAt(new Date())
                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                        + 1000 * 60 * 60 * 24)) // 24 hours
                .claim("email", auth.getName())
                .claim("authorities", roles)
                .signWith(key)
                .compact();

        return jwt;
    }

    public static String getEmailFromJwtToken(String jwt) {

    	if(jwt.startsWith("Bearer ")) {
            jwt = jwt.substring(7);
        }
    	
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(jwt)
                .getPayload();

        return claims.get("email", String.class);
    }

    private static String populateAuthorities(
            Collection<? extends GrantedAuthority> authorities) {

        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
    }
}