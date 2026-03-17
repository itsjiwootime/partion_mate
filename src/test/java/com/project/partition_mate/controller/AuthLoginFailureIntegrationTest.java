package com.project.partition_mate.controller;

import com.project.partition_mate.domain.User;
import com.project.partition_mate.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthLoginFailureIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void 존재하지_않는_이메일로_로그인하면_공통_실패_응답을_반환한다() throws Exception {
        // given
        String payload = """
                {
                  "email": "missing@test.com",
                  "password": "password123"
                }
                """;

        // when
        // then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"))
                .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호를 확인해주세요."));
    }

    @Test
    void 비밀번호가_틀리면_공통_실패_응답을_반환한다() throws Exception {
        // given
        userRepository.save(new User(
                "로그인테스트",
                "member@test.com",
                passwordEncoder.encode("password123"),
                "서울시 송파구",
                37.51,
                127.11
        ));

        String payload = """
                {
                  "email": "member@test.com",
                  "password": "wrongpass"
                }
                """;

        // when
        // then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"))
                .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호를 확인해주세요."));
    }

    @Test
    void 비밀번호_길이가_짧으면_수정된_검증_문구를_반환한다() throws Exception {
        // given
        String payload = """
                {
                  "email": "member@test.com",
                  "password": "12345"
                }
                """;

        // when
        // then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("비밀번호는 6자 이상 20자 이하로 입력해주세요"));
    }
}
