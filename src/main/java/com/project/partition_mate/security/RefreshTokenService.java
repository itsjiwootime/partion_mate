package com.project.partition_mate.security;

import com.project.partition_mate.domain.RefreshToken;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.exception.CustomAuthException;
import com.project.partition_mate.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationTime;

    @Transactional
    public IssuedRefreshToken issue(User user) {
        String rawToken = generateRawToken();
        String tokenHash = hash(rawToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusNanos(refreshExpirationTime * 1_000_000L);

        refreshTokenRepository.save(new RefreshToken(user, tokenHash, expiresAt));

        return new IssuedRefreshToken(
                rawToken,
                expiresAt,
                Math.max(1L, refreshExpirationTime / 1_000L)
        );
    }

    @Transactional
    public IssuedRefreshToken rotate(String rawToken) {
        RefreshToken currentToken = getValidRefreshToken(rawToken);
        currentToken.revoke();
        return issue(currentToken.getUser());
    }

    @Transactional
    public Long findUserId(String rawToken) {
        return getValidRefreshToken(rawToken).getUser().getId();
    }

    @Transactional
    public void revoke(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            return;
        }

        refreshTokenRepository.findByTokenHashForUpdate(hash(rawToken))
                .ifPresent(token -> {
                    if (!token.isRevoked()) {
                        token.revoke();
                    }
                });
    }

    public String hashForTest(String rawToken) {
        return hash(rawToken);
    }

    private RefreshToken getValidRefreshToken(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            throw CustomAuthException.INVALID_REFRESH_TOKEN;
        }

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHashForUpdate(hash(rawToken))
                .orElseThrow(() -> CustomAuthException.INVALID_REFRESH_TOKEN);

        if (refreshToken.isRevoked()) {
            throw CustomAuthException.INVALID_REFRESH_TOKEN;
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshToken.revoke();
            throw CustomAuthException.INVALID_REFRESH_TOKEN;
        }

        return refreshToken;
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 해시를 생성할 수 없습니다.", ex);
        }
    }
}
