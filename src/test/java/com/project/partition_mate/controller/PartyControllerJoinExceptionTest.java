package com.project.partition_mate.controller;

import com.project.partition_mate.exception.BusinessException;
import com.project.partition_mate.exception.GlobalExceptionHandler;
import com.project.partition_mate.service.PartyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PartyControllerJoinExceptionTest {

    @Mock
    private PartyService partyService;

    @InjectMocks
    private PartyController partyController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(partyController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("이미 참여한 파티에 다시 참여하면 공통 예외 응답을 반환한다")
    void 이미_참여한_파티에_다시_참여하면_공통_예외_응답을_반환한다() throws Exception {
        // given
        given(partyService.joinParty(eq(1L), any()))
                .willThrow(BusinessException.alreadyJoined());

        // when
        // then
        mockMvc.perform(post("/party/1/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberRequestQuantity": 1
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"))
                .andExpect(jsonPath("$.message").value("이미 참여한 파티입니다."));
    }

    @Test
    @DisplayName("잔여 수량이 부족하면 공통 예외 응답을 반환한다")
    void 잔여_수량이_부족하면_공통_예외_응답을_반환한다() throws Exception {
        // given
        given(partyService.joinParty(eq(1L), any()))
                .willThrow(BusinessException.insufficientQuantity(0));

        // when
        // then
        mockMvc.perform(post("/party/1/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberRequestQuantity": 1
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"))
                .andExpect(jsonPath("$.message").value("남은 수량(0개)이 부족하여 참여할 수 없습니다."));
    }
}
