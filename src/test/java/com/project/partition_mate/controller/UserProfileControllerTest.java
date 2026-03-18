package com.project.partition_mate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.UserResponse;
import com.project.partition_mate.security.CustomUserDetails;
import com.project.partition_mate.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserProfileControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User currentUser;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new com.project.partition_mate.exception.GlobalExceptionHandler())
                .build();
        this.objectMapper = new ObjectMapper();
        this.currentUser = new User("tester", "tester@test.com", "pw", "서울", 37.5, 127.0);
        ReflectionTestUtils.setField(currentUser, "id", 1L);

        CustomUserDetails principal = new CustomUserDetails(currentUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 내_프로필을_수정한다() throws Exception {
        // given
        given(userService.updateProfile(eq(currentUser), any()))
                .willReturn(UserResponse.from(
                        new User("새닉네임", "tester@test.com", "pw", "서울 송파구", null, null),
                        null,
                        List.of()
                ));

        // when
        // then
        mockMvc.perform(patch("/api/users/me")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "새닉네임",
                                "address", "서울 송파구"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("새닉네임"))
                .andExpect(jsonPath("$.address").value("서울 송파구"));

        ArgumentCaptor<com.project.partition_mate.dto.UpdateUserProfileRequest> captor =
                ArgumentCaptor.forClass(com.project.partition_mate.dto.UpdateUserProfileRequest.class);
        then(userService).should().updateProfile(eq(currentUser), captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("새닉네임");
        assertThat(captor.getValue().getAddress()).isEqualTo("서울 송파구");
    }

    @Test
    void 좌표를_하나만_보내면_예외가_발생한다() throws Exception {
        // given

        // when
        // then
        mockMvc.perform(patch("/api/users/me")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "새닉네임",
                                "address", "서울 송파구",
                                "latitude", 37.5
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("위치 좌표는 위도와 경도를 함께 보내야 합니다."));
    }
}
