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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PartyControllerUpdateExceptionTest {

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
    void 필수값없이_파티를_수정하면_예외가_발생한다() throws Exception {
        // given
        String invalidBody = """
                {
                  "title": "",
                  "productName": "비타민 C 1000정",
                  "totalPrice": 32000,
                  "totalQuantity": 4,
                  "deadline": "2099-03-18T10:00:00",
                  "unitLabel": "통",
                  "minimumShareUnit": 1,
                  "storageType": "ROOM_TEMPERATURE",
                  "packagingType": "CONTAINER",
                  "hostProvidesPackaging": false,
                  "onSiteSplit": true,
                  "guideNote": "현장 분배 후 전달합니다."
                }
                """;

        // when
        // then
        mockMvc.perform(put("/party/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("제목은 필수 입니다."));
        verifyNoInteractions(partyService);
    }

    @Test
    void 호스트가_아니면_파티를_수정하면_예외가_발생한다() throws Exception {
        // given
        given(partyService.updateParty(eq(1L), any()))
                .willThrow(BusinessException.onlyHostCanUpdateParty());

        // when
        // then
        mockMvc.perform(put("/party/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "수정된 제목",
                                  "productName": "비타민 C 1000정",
                                  "totalPrice": 32000,
                                  "totalQuantity": 4,
                                  "deadline": "2099-03-18T10:00:00",
                                  "unitLabel": "통",
                                  "minimumShareUnit": 1,
                                  "storageType": "ROOM_TEMPERATURE",
                                  "packagingType": "CONTAINER",
                                  "hostProvidesPackaging": false,
                                  "onSiteSplit": true,
                                  "guideNote": "현장 분배 후 전달합니다."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"))
                .andExpect(jsonPath("$.message").value("파티 수정은 호스트만 처리할 수 있습니다."));
    }
}
