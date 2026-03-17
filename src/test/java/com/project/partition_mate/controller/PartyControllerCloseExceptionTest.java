package com.project.partition_mate.controller;

import com.project.partition_mate.exception.BusinessException;
import com.project.partition_mate.exception.GlobalExceptionHandler;
import com.project.partition_mate.service.PartyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PartyControllerCloseExceptionTest {

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
    void 진행중인_거래가_있으면_호스트_취소시_예외가_발생한다() throws Exception {
        // given
        given(partyService.closeParty(eq(1L)))
                .willThrow(BusinessException.partyHostCancelNotAllowedAfterOperationsStarted());

        // when
        // then
        mockMvc.perform(post("/party/1/close"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"))
                .andExpect(jsonPath("$.message").value("정산, 픽업, 송금 또는 거래가 시작된 파티는 호스트 취소할 수 없습니다."));
    }
}
