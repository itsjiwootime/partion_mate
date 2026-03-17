package com.project.partition_mate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.UserBlockResponse;
import com.project.partition_mate.security.CustomUserDetails;
import com.project.partition_mate.service.UserBlockService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserBlockControllerTest {

    @Mock
    private UserBlockService userBlockService;

    @InjectMocks
    private UserBlockController userBlockController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User currentUser;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(userBlockController)
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
    void 사용자를_차단한다() throws Exception {
        // given
        given(userBlockService.blockUser(eq(currentUser), eq(3L))).willReturn(createResponse(10L, 3L, "blocked-user"));

        // when
        // then
        mockMvc.perform(post("/api/blocks")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("targetUserId", 3L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.targetUserId").value(3))
                .andExpect(jsonPath("$.targetUsername").value("blocked-user"));
    }

    @Test
    void 차단_목록을_조회한다() throws Exception {
        // given
        given(userBlockService.getBlockedUsers(currentUser))
                .willReturn(List.of(createResponse(11L, 4L, "blocked-user")));

        // when
        // then
        mockMvc.perform(get("/api/blocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(11))
                .andExpect(jsonPath("$[0].targetUserId").value(4))
                .andExpect(jsonPath("$[0].targetUsername").value("blocked-user"))
                .andExpect(jsonPath("$[0].createdAtLabel").value("2026.03.18 03:00"));
    }

    @Test
    void 사용자를_차단해제한다() throws Exception {
        // given

        // when
        // then
        mockMvc.perform(delete("/api/blocks/5"))
                .andExpect(status().isNoContent());

        then(userBlockService).should().unblockUser(currentUser, 5L);
        then(userBlockService).should(never()).getBlockedUsers(currentUser);
    }

    private UserBlockResponse createResponse(Long id, Long targetUserId, String username) {
        User blocked = new User(username, username + "@test.com", "pw", "서울", 37.5, 127.0);
        ReflectionTestUtils.setField(blocked, "id", targetUserId);
        com.project.partition_mate.domain.UserBlock userBlock = com.project.partition_mate.domain.UserBlock.create(
                currentUser,
                blocked,
                LocalDateTime.of(2026, 3, 18, 3, 0)
        );
        ReflectionTestUtils.setField(userBlock, "id", id);
        return UserBlockResponse.from(userBlock);
    }
}
