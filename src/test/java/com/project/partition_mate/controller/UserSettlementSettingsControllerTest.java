package com.project.partition_mate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.SettlementSettingsResponse;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserSettlementSettingsControllerTest {

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
    void 내_정산_기본_안내를_조회한다() throws Exception {
        // given
        given(userService.getSettlementSettings(currentUser))
                .willReturn(SettlementSettingsResponse.from(settlementGuideUser("입금 후 채팅에 마지막 4자리를 남겨주세요.")));

        // when
        // then
        mockMvc.perform(get("/api/users/me/settlement-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settlementGuide").value("입금 후 채팅에 마지막 4자리를 남겨주세요."));
    }

    @Test
    void 내_정산_기본_안내를_수정한다() throws Exception {
        // given
        given(userService.updateSettlementSettings(eq(currentUser), any()))
                .willReturn(SettlementSettingsResponse.from(settlementGuideUser("무통장 입금 후 채팅 공지를 확인해주세요.")));

        // when
        // then
        mockMvc.perform(put("/api/users/me/settlement-settings")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "settlementGuide", "무통장 입금 후 채팅 공지를 확인해주세요."
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settlementGuide").value("무통장 입금 후 채팅 공지를 확인해주세요."));

        ArgumentCaptor<com.project.partition_mate.dto.UpdateSettlementSettingsRequest> captor =
                ArgumentCaptor.forClass(com.project.partition_mate.dto.UpdateSettlementSettingsRequest.class);
        then(userService).should().updateSettlementSettings(eq(currentUser), captor.capture());
        assertThat(captor.getValue().getSettlementGuide()).isEqualTo("무통장 입금 후 채팅 공지를 확인해주세요.");
    }

    @Test
    void 정산_기본_안내가_300자를_넘기면_예외가_발생한다() throws Exception {
        // given
        String tooLongGuide = "a".repeat(301);

        // when
        // then
        mockMvc.perform(put("/api/users/me/settlement-settings")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "settlementGuide", tooLongGuide
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("정산 기본 안내는 300자 이하로 입력해 주세요."));
    }

    private User settlementGuideUser(String settlementGuide) {
        User user = new User("tester", "tester@test.com", "pw", "서울", 37.5, 127.0);
        user.updateSettlementGuide(settlementGuide);
        return user;
    }
}
