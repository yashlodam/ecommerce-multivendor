package com.zosh.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.zosh.config.AppConfig;
import com.zosh.config.CorsConfig;
import com.zosh.config.JwtTokenValidator;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebMvcTest(controllers = HealthController.class)
@Import({AppConfig.class, CorsConfig.class})
class HealthSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenValidator jwtTokenValidator;

    @BeforeEach
    void setUp() throws Exception {
        Mockito.doAnswer(invocation -> {
            HttpServletRequest req = invocation.getArgument(0);
            HttpServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtTokenValidator).doFilter(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    @DisplayName("GET /health: without JWT token returns HTTP 200 and {\"status\":\"UP\"}")
    void getHealth_withoutToken_isPermitted() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("HEAD /health: without JWT token returns HTTP 200 (for UptimeRobot HEAD pingers)")
    void headHealth_withoutToken_isPermitted() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head("/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/users/profile: without JWT token returns HTTP 401 UNAUTHORIZED")
    void getProtectedEndpoint_withoutToken_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("GET /sellers/profile: without JWT token returns HTTP 401 UNAUTHORIZED")
    void getSellerProtectedEndpoint_withoutToken_isUnauthorized() throws Exception {
        mockMvc.perform(get("/sellers/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("GET /api/admin/coupons: without JWT token returns HTTP 401 UNAUTHORIZED")
    void getAdminProtectedEndpoint_withoutToken_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/coupons"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
