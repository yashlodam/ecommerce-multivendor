package com.zosh.config;

/**
 * JWT constants — header name only.
 * The secret key is injected via @Value in JwtProvider and JwtTokenValidator
 * from the application property 'app.jwt.secret' which is sourced from
 * the JWT_SECRET_KEY environment variable.
 *
 * DO NOT put a real secret key in this class.
 */
public final class JWT_CONSTANT {

    private JWT_CONSTANT() {}

    public static final String JWT_HEADER = "Authorization";
    public static final String DEFAULT_SECRET_KEY = "ShopSphereDefaultSecretKeyForTokenSigningInDevelopmentAndStagingOnlyMustBeLongEnough64Bytes";
}
