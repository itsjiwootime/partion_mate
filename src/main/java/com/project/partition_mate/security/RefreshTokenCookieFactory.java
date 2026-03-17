package com.project.partition_mate.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookieFactory {

    private final RefreshTokenCookieProperties refreshTokenCookieProperties;

    public ResponseCookie create(String refreshToken, long maxAgeSeconds) {
        return ResponseCookie.from(refreshTokenCookieProperties.getCookieName(), refreshToken)
                .httpOnly(true)
                .secure(refreshTokenCookieProperties.isCookieSecure())
                .sameSite(refreshTokenCookieProperties.getCookieSameSite())
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }

    public ResponseCookie clear() {
        return ResponseCookie.from(refreshTokenCookieProperties.getCookieName(), "")
                .httpOnly(true)
                .secure(refreshTokenCookieProperties.isCookieSecure())
                .sameSite(refreshTokenCookieProperties.getCookieSameSite())
                .path("/")
                .maxAge(0)
                .build();
    }
}
