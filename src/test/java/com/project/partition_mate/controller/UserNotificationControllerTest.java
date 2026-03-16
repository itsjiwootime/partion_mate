package com.project.partition_mate.controller;

import com.project.partition_mate.domain.User;
import com.project.partition_mate.domain.UserNotification;
import com.project.partition_mate.domain.UserNotificationType;
import com.project.partition_mate.dto.UserNotificationResponse;
import com.project.partition_mate.security.CustomUserDetails;
import com.project.partition_mate.service.UserService;
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

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserNotificationControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private User currentUser;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        this.currentUser = new User("tester", "tester@test.com", "pw", "서울", 37.5, 127.0);
        ReflectionTestUtils.setField(currentUser, "id", 1L);

        CustomUserDetails principal = new CustomUserDetails(currentUser);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 내_알림_목록을_조회한다() throws Exception {
        // given
        UserNotification notification = UserNotification.create(
                currentUser,
                UserNotificationType.PARTY_CLOSED,
                "파티 모집이 종료되었습니다",
                "비타민 파티가 종료되었습니다.",
                "/parties/1",
                "event-1:1:PARTY_CLOSED",
                LocalDateTime.of(2026, 3, 16, 2, 30)
        );
        ReflectionTestUtils.setField(notification, "id", 10L);

        given(userService.getNotifications(currentUser))
                .willReturn(List.of(UserNotificationResponse.from(notification)));

        // when
        // then
        mockMvc.perform(get("/api/users/me/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].type").value("PARTY_CLOSED"))
                .andExpect(jsonPath("$[0].title").value("파티 모집이 종료되었습니다"))
                .andExpect(jsonPath("$[0].message").value("비타민 파티가 종료되었습니다."))
                .andExpect(jsonPath("$[0].linkUrl").value("/parties/1"))
                .andExpect(jsonPath("$[0].createdAtLabel").value("2026.03.16 02:30"));
    }
}
