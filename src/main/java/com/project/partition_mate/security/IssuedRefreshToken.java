package com.project.partition_mate.security;

import java.time.LocalDateTime;

public record IssuedRefreshToken(
        String tokenValue,
        LocalDateTime expiresAt,
        long maxAgeSeconds
) {
}
