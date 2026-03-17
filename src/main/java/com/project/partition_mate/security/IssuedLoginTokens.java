package com.project.partition_mate.security;

public record IssuedLoginTokens(
        String accessToken,
        long accessTokenExpiresInMs,
        IssuedRefreshToken refreshToken
) {
}
