package com.project.partition_mate.controller;

import com.project.partition_mate.domain.RefreshToken;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.repository.RefreshTokenRepository;
import com.project.partition_mate.repository.UserRepository;
import com.project.partition_mate.security.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthRefreshTokenFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 유효한_refresh_token으로_access_token을_재발급하고_rotation한다() throws Exception {
        // given
        User user = 사용자_생성("refresh@test.com");
        String originalRefreshToken = 로그인하고_refresh_token을_추출한다(user.getEmail());

        // when
        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie(originalRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.accessTokenExpiresInMs").value(3600000))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("pm_refresh_token=")))
                .andReturn();

        String rotatedRefreshToken = extractCookieValue(result.getResponse().getHeader(HttpHeaders.SET_COOKIE));
        RefreshToken revokedToken = refreshTokenRepository.findByTokenHash(refreshTokenService.hashForTest(originalRefreshToken))
                .orElseThrow();
        RefreshToken activeToken = refreshTokenRepository.findByTokenHash(refreshTokenService.hashForTest(rotatedRefreshToken))
                .orElseThrow();

        // then
        assertThat(rotatedRefreshToken).isNotEqualTo(originalRefreshToken);
        assertThat(revokedToken.isRevoked()).isTrue();
        assertThat(activeToken.isRevoked()).isFalse();
        assertThat(refreshTokenRepository.findAllByUserId(user.getId())).hasSize(2);
    }

    @Test
    void 회전된_refresh_token을_다시_사용하면_재발급에_실패한다() throws Exception {
        // given
        User user = 사용자_생성("reused@test.com");
        String originalRefreshToken = 로그인하고_refresh_token을_추출한다(user.getEmail());
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie(originalRefreshToken)))
                .andExpect(status().isOk());

        // when
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie(originalRefreshToken)))
                // then
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
                .andExpect(jsonPath("$.message").value("세션이 만료되었습니다. 다시 로그인해주세요."));
    }

    @Test
    void 로그아웃하면_refresh_token이_무효화되고_재발급할_수_없다() throws Exception {
        // given
        User user = 사용자_생성("logout@test.com");
        String refreshToken = 로그인하고_refresh_token을_추출한다(user.getEmail());

        // when
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(refreshCookie(refreshToken)))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));

        // then
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie(refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        List<RefreshToken> refreshTokens = refreshTokenRepository.findAllByUserId(user.getId());
        assertThat(refreshTokens).hasSize(1);
        assertThat(refreshTokens.getFirst().isRevoked()).isTrue();
    }

    private User 사용자_생성(String email) {
        return userRepository.save(new User(
                "리프레시테스트유저",
                email,
                passwordEncoder.encode("password123"),
                "서울시 강동구",
                37.54,
                127.13
        ));
    }

    private String 로그인하고_refresh_token을_추출한다(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "password123"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();

        return extractCookieValue(result.getResponse().getHeader(HttpHeaders.SET_COOKIE));
    }

    private Cookie refreshCookie(String refreshTokenValue) {
        Cookie cookie = new Cookie("pm_refresh_token", refreshTokenValue);
        cookie.setPath("/");
        return cookie;
    }

    private String extractCookieValue(String cookieHeader) {
        return cookieHeader.substring(cookieHeader.indexOf('=') + 1, cookieHeader.indexOf(';'));
    }
}
