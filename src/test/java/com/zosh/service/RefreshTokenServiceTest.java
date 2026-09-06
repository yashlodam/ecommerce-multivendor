package com.zosh.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

import com.zosh.model.RefreshToken;
import com.zosh.repository.RefreshTokenRepository;
import com.zosh.service.impl.RefreshTokenServiceImpl;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    private static final String TEST_EMAIL = "customer@example.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpirationDays", 7);
    }

    @Test
    @DisplayName("createRefreshToken should persist and return a valid token")
    void createRefreshToken_success() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken token = refreshTokenService.createRefreshToken(TEST_EMAIL);

        assertNotNull(token);
        assertNotNull(token.getToken());
        assertEquals(TEST_EMAIL, token.getEmail());
        assertFalse(token.isRevoked());
        assertTrue(token.getExpiryDate().isAfter(Instant.now()));
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("verifyExpiration should succeed for a valid non-expired token")
    void verifyExpiration_valid() {
        RefreshToken token = new RefreshToken("token-123", TEST_EMAIL, Instant.now().plus(Duration.ofDays(2)));
        RefreshToken verified = refreshTokenService.verifyExpiration(token);
        assertEquals(token, verified);
    }

    @Test
    @DisplayName("verifyExpiration should delete and throw for an expired token")
    void verifyExpiration_expired() {
        RefreshToken token = new RefreshToken("token-123", TEST_EMAIL, Instant.now().minus(Duration.ofDays(1)));

        assertThrows(BadCredentialsException.class, () -> refreshTokenService.verifyExpiration(token));
        verify(refreshTokenRepository, times(1)).delete(token);
    }

    @Test
    @DisplayName("verifyExpiration should throw for a revoked token")
    void verifyExpiration_revoked() {
        RefreshToken token = new RefreshToken("token-123", TEST_EMAIL, Instant.now().plus(Duration.ofDays(2)));
        token.setRevoked(true);

        assertThrows(BadCredentialsException.class, () -> refreshTokenService.verifyExpiration(token));
    }

    @Test
    @DisplayName("rotateRefreshToken should revoke old token and return a fresh token")
    void rotateRefreshToken_success() {
        RefreshToken oldToken = new RefreshToken("old-token-xyz", TEST_EMAIL, Instant.now().plus(Duration.ofDays(2)));
        when(refreshTokenRepository.findByToken("old-token-xyz")).thenReturn(Optional.of(oldToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken rotated = refreshTokenService.rotateRefreshToken("old-token-xyz");

        assertNotNull(rotated);
        assertNotEquals("old-token-xyz", rotated.getToken());
        assertEquals(TEST_EMAIL, rotated.getEmail());
        assertTrue(oldToken.isRevoked());
        verify(refreshTokenRepository, atLeast(2)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("rotateRefreshToken should detect reuse of revoked token and delete all user tokens")
    void rotateRefreshToken_reuseDetection() {
        RefreshToken replayedToken = new RefreshToken("stolen-token", TEST_EMAIL, Instant.now().plus(Duration.ofDays(2)));
        replayedToken.setRevoked(true);

        when(refreshTokenRepository.findByToken("stolen-token")).thenReturn(Optional.of(replayedToken));

        assertThrows(BadCredentialsException.class, () -> refreshTokenService.rotateRefreshToken("stolen-token"));
        verify(refreshTokenRepository, times(1)).deleteByEmail(TEST_EMAIL);
    }

    @Test
    @DisplayName("revokeToken should set revoked flag to true")
    void revokeToken_success() {
        RefreshToken token = new RefreshToken("token-to-revoke", TEST_EMAIL, Instant.now().plus(Duration.ofDays(2)));
        when(refreshTokenRepository.findByToken("token-to-revoke")).thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        refreshTokenService.revokeToken("token-to-revoke");

        assertTrue(token.isRevoked());
        verify(refreshTokenRepository, times(1)).save(token);
    }
}
