package com.project.partition_mate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.partition_mate.domain.ReportReasonType;
import com.project.partition_mate.domain.ReportStatus;
import com.project.partition_mate.domain.ReportTargetType;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.ReportResponse;
import com.project.partition_mate.security.CustomUserDetails;
import com.project.partition_mate.service.ReportService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController reportController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User currentUser;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(reportController).setControllerAdvice(new com.project.partition_mate.exception.GlobalExceptionHandler()).build();
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
    void 신고를_접수한다() throws Exception {
        // given
        ReportResponse response = createResponse(10L, ReportTargetType.USER, 3L, "비타민 파티", 2L, "host");
        given(reportService.createReport(eq(currentUser), any())).willReturn(response);

        // when
        // then
        mockMvc.perform(post("/api/reports")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "targetType", "USER",
                                "partyId", 3L,
                                "targetUserId", 2L,
                                "reasonType", "PAYMENT_ISSUE",
                                "memo", "정산 안내가 달랐습니다."
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.targetType").value("USER"))
                .andExpect(jsonPath("$.partyId").value(3))
                .andExpect(jsonPath("$.targetUserId").value(2))
                .andExpect(jsonPath("$.reasonType").value("PAYMENT_ISSUE"))
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    void 내_신고_목록을_조회한다() throws Exception {
        // given
        given(reportService.getMyReports(currentUser))
                .willReturn(List.of(createResponse(11L, ReportTargetType.PARTY, 5L, "휴지 파티", null, null)));

        // when
        // then
        mockMvc.perform(get("/api/reports/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(11))
                .andExpect(jsonPath("$[0].targetType").value("PARTY"))
                .andExpect(jsonPath("$[0].partyTitle").value("휴지 파티"))
                .andExpect(jsonPath("$[0].status").value("RECEIVED"))
                .andExpect(jsonPath("$[0].createdAtLabel").value("2026.03.18 02:30"));
    }

    private ReportResponse createResponse(Long id,
                                          ReportTargetType targetType,
                                          Long partyId,
                                          String partyTitle,
                                          Long targetUserId,
                                          String targetUsername) {
        User targetUser = null;
        if (targetUserId != null) {
            targetUser = new User(targetUsername, targetUsername + "@test.com", "pw", "서울", 37.5, 127.0);
            ReflectionTestUtils.setField(targetUser, "id", targetUserId);
        }

        com.project.partition_mate.domain.Report report = com.project.partition_mate.domain.Report.create(
                currentUser,
                createParty(partyId, partyTitle),
                targetUser,
                targetType,
                ReportReasonType.PAYMENT_ISSUE,
                "테스트 메모",
                LocalDateTime.of(2026, 3, 18, 2, 30)
        );
        ReflectionTestUtils.setField(report, "id", id);
        ReflectionTestUtils.setField(report, "status", ReportStatus.RECEIVED);
        return ReportResponse.from(report);
    }

    private com.project.partition_mate.domain.Party createParty(Long id, String title) {
        com.project.partition_mate.domain.Store store =
                new com.project.partition_mate.domain.Store("테스트 지점", "서울", java.time.LocalTime.of(10, 0), java.time.LocalTime.of(22, 0), 37.5, 127.0, "02-0000-0000");
        com.project.partition_mate.domain.Party party = new com.project.partition_mate.domain.Party(
                title,
                "테스트 상품",
                10000,
                store,
                2,
                "https://open.kakao.com/o/test",
                LocalDateTime.of(2026, 3, 18, 12, 0)
        );
        ReflectionTestUtils.setField(party, "id", id);
        return party;
    }
}
