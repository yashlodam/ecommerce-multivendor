package com.zosh.controller;

import java.util.Collections;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Lightweight, zero-overhead health check controller.
 *
 * Dedicated for UptimeRobot, Render uptime pingers, and load balancer probes.
 * Returns HTTP 200 with {"status": "UP"}.
 *
 * Guaranteed characteristics:
 * - No authentication / JWT required
 * - Zero database queries or network calls
 * - Constant-time, in-memory response (< 1ms)
 * - No sensitive data, secrets, or internal state exposed
 */
@RestController
@Tag(name = "Health", description = "Lightweight availability and uptime monitoring endpoint")
public class HealthController {

    private static final Map<String, String> HEALTH_RESPONSE = Collections.singletonMap("status", "UP");

    @GetMapping("/health")
    @Operation(summary = "Health check endpoint for external pingers and availability monitoring")
    public ResponseEntity<Map<String, String>> checkHealth() {
        return ResponseEntity.ok(HEALTH_RESPONSE);
    }
}
