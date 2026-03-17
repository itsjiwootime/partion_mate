package com.project.partition_mate.controller;

import com.project.partition_mate.domain.RefreshToken;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.repository.RefreshTokenRepository;
import com.project.partition_mate.repository.UserRepository;
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
class AuthLoginRefreshTokenIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 로그인에_성공하면_refresh_token_쿠키와_저장_레코드를_생성한다() throws Exception {
        // given
        User user = userRepository.save(new User(
                "테스트유저",
                "login@test.com",
                passwordEncoder.encode("password123"),
                "서울시 강남구",
                37.50,
                127.00
        ));

        // when
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login@test.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.accessTokenExpiresInMs").value(3600000))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("pm_refresh_token=")))
                .andReturn();

        String cookieHeader = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String refreshTokenValue = extractCookieValue(cookieHeader);
        List<RefreshToken> refreshTokens = refreshTokenRepository.findAll();

        // then
        assertThat(cookieHeader)
                .contains("HttpOnly")
                .contains("Path=/")
                .contains("SameSite=Lax")
                .contains("Max-Age=1209600");
        assertThat(refreshTokens).hasSize(1);
        assertThat(refreshTokens.getFirst().getUser().getId()).isEqualTo(user.getId());
        assertThat(refreshTokens.getFirst().getTokenHash()).isNotEqualTo(refreshTokenValue);
    }

    @Test
    void 같은_사용자는_여러기기용_refresh_token을_동시에_가질_수_있다() throws Exception {
        // given
        User user = userRepository.save(new User(
                "멀티로그인유저",
                "multi@test.com",
                passwordEncoder.encode("password123"),
                "서울시 성동구",
                37.54,
                127.04
        ));

        // when
        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "multi@test.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "multi@test.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk());

        // then
        assertThat(refreshTokenRepository.countByUserId(user.getId())).isEqualTo(2);
        assertThat(refreshTokenRepository.findAll())
                .extracting(RefreshToken::getTokenHash)
                .doesNotHaveDuplicates();
    }

    private String extractCookieValue(String cookieHeader) {
        return cookieHeader.substring(cookieHeader.indexOf('=') + 1, cookieHeader.indexOf(';'));
    }
}
