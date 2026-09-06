package com.zosh.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zosh.model.RefreshToken;
import com.zosh.repository.RefreshTokenRepository;
import com.zosh.service.RefreshTokenService;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);

    private static final Duration ROTATION_GRACE_PERIOD = Duration.ofSeconds(60);

    /**
     * In-memory record tracking recently rotated tokens and their newly issued active tokens.
     */
    private static class RotatedTokenGraceRecord {
        private final RefreshToken activeToken;
        private final Instant rotatedAt;

        public RotatedTokenGraceRecord(RefreshToken activeToken, Instant rotatedAt) {
            this.activeToken = activeToken;
            this.rotatedAt = rotatedAt;
        }

        public RefreshToken getActiveToken() {
            return activeToken;
        }

        public Instant getRotatedAt() {
            return rotatedAt;
        }
    }

    /**
     * Cache mapping oldTokenString -> RotatedTokenGraceRecord.
     * Prevents race conditions when multiple concurrent requests (e.g. React StrictMode,
     * multi-tab mounts, parallel 401 interceptors) present the same just-rotated token.
     */
    private final Map<String, RotatedTokenGraceRecord> gracePeriodCache = new ConcurrentHashMap<>();

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-token-expiration-days:7}")
    private int refreshTokenExpirationDays;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(String email) {
        String tokenString = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        Instant expiryDate = Instant.now().plus(Duration.ofDays(refreshTokenExpirationDays));

        RefreshToken refreshToken = new RefreshToken(tokenString, email, expiryDate);
        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token == null) {
            throw new BadCredentialsException("Refresh token not found");
        }

        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new BadCredentialsException("Refresh token has expired. Please login again.");
        }

        if (token.isRevoked()) {
            throw new BadCredentialsException("Refresh token has been revoked. Please login again.");
        }

        return token;
    }

    /**
     * Enforces Refresh Token Rotation with RFC 6819 / OAuth 2.0 BCP Grace Period.
     * Validates the token, invalidates it to prevent replay, and issues a fresh one.
     * If an already-rotated token is presented within the grace period (60s), the current
     * active token is safely returned rather than destroying user sessions.
     */
    @Override
    @Transactional
    public RefreshToken rotateRefreshToken(String oldTokenStr) {
        if (oldTokenStr == null || oldTokenStr.isBlank()) {
            throw new BadCredentialsException("Refresh token is required.");
        }

        // Synchronize on the specific token string to serialize rapid concurrent requests
        synchronized (oldTokenStr.intern()) {
            // 1. Fast in-memory grace check: handles concurrent requests in milliseconds
            RotatedTokenGraceRecord cachedRecord = gracePeriodCache.get(oldTokenStr);
            if (cachedRecord != null) {
                Duration elapsed = Duration.between(cachedRecord.getRotatedAt(), Instant.now());
                if (elapsed.compareTo(ROTATION_GRACE_PERIOD) <= 0 && !cachedRecord.getActiveToken().isExpired()) {
                    log.info("Concurrent refresh detected within in-memory grace period ({}ms ago) for token. Returning active token.",
                            elapsed.toMillis());
                    return cachedRecord.getActiveToken();
                }
            }

            RefreshToken oldToken = refreshTokenRepository.findByToken(oldTokenStr)
                    .orElseThrow(() -> new BadCredentialsException("Invalid refresh token."));

            // 2. If already revoked, verify whether it falls within the rotation grace period
            if (oldToken.isRevoked()) {
                // Check if an active token was recently created for this user (handles cross-server/restarts)
                Optional<RefreshToken> activeTokenOpt = refreshTokenRepository
                        .findTopByEmailAndRevokedFalseOrderByCreatedAtDesc(oldToken.getEmail());

                if (activeTokenOpt.isPresent()) {
                    RefreshToken activeToken = activeTokenOpt.get();
                    Duration diff = Duration.between(activeToken.getCreatedAt(), Instant.now());
                    if (diff.compareTo(ROTATION_GRACE_PERIOD) <= 0 && !activeToken.isExpired()) {
                        log.info("Concurrent refresh detected within DB grace period ({}s ago) for email: {}. Returning active token.",
                                diff.toSeconds(), oldToken.getEmail());
                        gracePeriodCache.put(oldTokenStr, new RotatedTokenGraceRecord(activeToken, activeToken.getCreatedAt()));
                        return activeToken;
                    }
                }

                // Outside grace period: real replay/token theft detected
                log.warn("Security Alert: Replayed/Revoked refresh token detected for email: {} outside grace period. Revoking all sessions.",
                        oldToken.getEmail());
                refreshTokenRepository.deleteByEmail(oldToken.getEmail());
                throw new BadCredentialsException("Invalid or already used refresh token. Please login again.");
            }

            if (oldToken.isExpired()) {
                refreshTokenRepository.delete(oldToken);
                throw new BadCredentialsException("Refresh token has expired. Please login again.");
            }

            // 3. Revoke the old token
            oldToken.setRevoked(true);
            refreshTokenRepository.save(oldToken);

            // 4. Issue and persist a fresh rotated token
            RefreshToken newToken = createRefreshToken(oldToken.getEmail());

            // 5. Cache rotated token under the old token string for the grace period window
            gracePeriodCache.put(oldTokenStr, new RotatedTokenGraceRecord(newToken, Instant.now()));
            purgeOldGraceRecords();

            return newToken;
        }
    }

    @Override
    @Transactional
    public void revokeToken(String tokenStr) {
        if (tokenStr != null && !tokenStr.isBlank()) {
            gracePeriodCache.remove(tokenStr);
            refreshTokenRepository.findByToken(tokenStr).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
        }
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(String email) {
        if (email != null && !email.isBlank()) {
            refreshTokenRepository.deleteByEmail(email);
            gracePeriodCache.entrySet().removeIf(entry -> email.equalsIgnoreCase(entry.getValue().getActiveToken().getEmail()));
        }
    }

    private void purgeOldGraceRecords() {
        if (gracePeriodCache.size() > 100) {
            Instant threshold = Instant.now().minus(Duration.ofMinutes(5));
            gracePeriodCache.entrySet().removeIf(entry -> entry.getValue().getRotatedAt().isBefore(threshold));
        }
    }
}
