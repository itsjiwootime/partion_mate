package com.project.partition_mate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.domain.UserNotificationType;
import com.project.partition_mate.dto.NotificationPreferenceResponse;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserNotificationPreferenceControllerTest {

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
    void 내_알림설정을_조회한다() throws Exception {
        // given
        given(userService.getNotificationPreferences(currentUser))
                .willReturn(List.of(NotificationPreferenceResponse.of(UserNotificationType.PICKUP_UPDATED, true)));

        // when
        // then
        mockMvc.perform(get("/api/users/me/notification-preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("PICKUP_UPDATED"))
                .andExpect(jsonPath("$[0].label").value("픽업 일정 확정"))
                .andExpect(jsonPath("$[0].webPushSupported").value(true))
                .andExpect(jsonPath("$[0].webPushEnabled").value(true))
                .andExpect(jsonPath("$[0].deepLinkTargetLabel").value("파티 상세"));
    }

    @Test
    void 내_알림설정을_수정한다() throws Exception {
        // given
        given(userService.updateNotificationPreferences(eq(currentUser), any()))
                .willReturn(List.of(NotificationPreferenceResponse.of(UserNotificationType.PICKUP_UPDATED, false)));

        // when
        // then
        mockMvc.perform(put("/api/users/me/notification-preferences")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "preferences", List.of(
                                        Map.of(
                                                "type", "PICKUP_UPDATED",
                                                "webPushEnabled", false
                                        )
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("PICKUP_UPDATED"))
                .andExpect(jsonPath("$[0].webPushEnabled").value(false));

        ArgumentCaptor<com.project.partition_mate.dto.UpdateNotificationPreferencesRequest> captor =
                ArgumentCaptor.forClass(com.project.partition_mate.dto.UpdateNotificationPreferencesRequest.class);
        then(userService).should().updateNotificationPreferences(eq(currentUser), captor.capture());
        assertThat(captor.getValue().getPreferences()).hasSize(1);
        assertThat(captor.getValue().getPreferences().getFirst().getType()).isEqualTo(UserNotificationType.PICKUP_UPDATED);
        assertThat(captor.getValue().getPreferences().getFirst().isWebPushEnabled()).isFalse();
    }
}
