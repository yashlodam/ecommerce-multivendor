package com.zosh.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS configuration for ShopSphere API.
 *
 * Explicitly provides a CorsConfigurationSource bean for Spring Security 6,
 * as well as a CorsFilter bean.
 *
 * Allowed origin patterns include localhost, Vercel deployments (*.vercel.app),
 * the production Vercel frontend, and any custom origins passed via APP_CORS_ALLOWED_ORIGINS / FRONTEND_URL.
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173,https://*.vercel.app,https://shopsphereecommerceweb.vercel.app}")
    private String allowedOriginsRaw;

    @Bean
    @Primary
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Safe defaults that are ALWAYS permitted
        List<String> origins = new ArrayList<>(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://localhost:5174",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:5173",
                "https://*.vercel.app",
                "https://shopsphereecommerceweb.vercel.app"
        ));

        // Add any configured origins from environment variables or application.properties
        if (allowedOriginsRaw != null && !allowedOriginsRaw.isBlank()) {
            Arrays.stream(allowedOriginsRaw.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && !origins.contains(s))
                    .forEach(origins::add);
        }

        config.setAllowedOriginPatterns(origins);

        config.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));

        config.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "Cache-Control",
                "X-Auth-Token",
                "x-refresh-token",
                "Pragma"));

        config.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Set-Cookie",
                "x-refresh-token"));

        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // pre-flight cache for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }

    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }
}
