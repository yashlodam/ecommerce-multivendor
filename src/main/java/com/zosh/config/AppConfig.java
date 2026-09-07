package com.zosh.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Central Spring Security configuration.
 *
 * Role-based URL authorization:
 *   /api/admin/**  → ROLE_ADMIN only
 *   /seller/**     → ROLE_SELLER only (with method-level checks for fine-grained isolation)
 *   /api/**        → any authenticated user
 *   /auth/**       → public (signup/login OTP flow)
 *   /products/**   → public (catalog browsing)
 *
 * Sessions are stateless (JWT-based).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class AppConfig {

    @Autowired
    private JwtTokenValidator jwtTokenValidator;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> cors.configurationSource(corsConfigurationSource))                     // CorsConfig bean handles CORS
            .csrf(csrf -> csrf.disable())          // CSRF not needed for stateless JWT APIs
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"status\":401,\"code\":\"UNAUTHORIZED\",\"message\":\"Authentication required. Please provide a valid Bearer token.\"}"
                    );
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"status\":403,\"code\":\"FORBIDDEN\",\"message\":\"Access denied. You do not have permission to access this resource.\"}"
                    );
                })
            )
            .authorizeHttpRequests(auth -> auth
                // Public endpoints — no token required
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/home/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/*/reviews").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/deals/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/coupons/active").permitAll()
                .requestMatchers(HttpMethod.GET, "/").permitAll()
                .requestMatchers("/health").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()

                // AI Shopping Assistant — session-level auth handled in service
                .requestMatchers("/api/chat/**").permitAll()

                // Swagger / OpenAPI documentation
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/v3/api-docs.yaml",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()

                // Admin-only endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/home/categories").hasRole("ADMIN")

                // Seller-only endpoints
                .requestMatchers("/sellers/products/**").hasRole("SELLER")
                .requestMatchers("/sellers/deals/**").hasRole("SELLER")
                .requestMatchers("/seller/orders/**").hasRole("SELLER")
                .requestMatchers("/api/transactions/seller").hasRole("SELLER")
                .requestMatchers("/sellers/report").hasRole("SELLER")
                .requestMatchers("/sellers/profile").hasRole("SELLER")

                // Admin-only deal/coupon/home-category management
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // Any other /api/** requires authentication
                .requestMatchers("/api/**").authenticated()

                // Seller registration and login are public
                .requestMatchers(HttpMethod.POST, "/sellers/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/sellers").permitAll()
                .requestMatchers(HttpMethod.PATCH, "/sellers/verify/**").permitAll()

                // Default
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtTokenValidator, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}