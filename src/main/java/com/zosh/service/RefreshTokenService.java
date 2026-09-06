package com.zosh.service;

import com.zosh.model.RefreshToken;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(String email);

    RefreshToken verifyExpiration(RefreshToken token);

    RefreshToken rotateRefreshToken(String oldTokenStr);

    void revokeToken(String tokenStr);

    void revokeAllUserTokens(String email);
}
