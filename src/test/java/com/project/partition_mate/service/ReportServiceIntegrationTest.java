package com.project.partition_mate.service;

import com.project.partition_mate.domain.PackagingType;
import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.domain.Report;
import com.project.partition_mate.domain.ReportReasonType;
import com.project.partition_mate.domain.ReportStatus;
import com.project.partition_mate.domain.ReportTargetType;
import com.project.partition_mate.domain.StorageType;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.exception.BusinessException;
import com.project.partition_mate.dto.CreateReportRequest;
import com.project.partition_mate.dto.ReportResponse;
import com.project.partition_mate.repository.PartyMemberRepository;
import com.project.partition_mate.repository.PartyRepository;
import com.project.partition_mate.repository.ReportRepository;
import com.project.partition_mate.repository.StoreRepository;
import com.project.partition_mate.repository.UserRepository;
import com.project.partition_mate.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
@ActiveProfiles("test")
class ReportServiceIntegrationTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private PartyMemberRepository partyMemberRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        reportRepository.deleteAll();
        partyMemberRepository.deleteAll();
        partyRepository.deleteAll();
        storeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 참여자가_상대사용자를_신고하면_접수한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("신고 지점"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User member = userRepository.saveAndFlush(createUser("member"));
        Party party = createParty(store, host, member);
        setAuth(member);
        CreateReportRequest request = createRequest(
                ReportTargetType.USER,
                party.getId(),
                host.getId(),
                ReportReasonType.PAYMENT_ISSUE,
                "정산 금액 안내와 실제 요청 내용이 다릅니다."
        );

        // when
        ReportResponse response = reportService.createReport(member, request);

        // then
        Report savedReport = reportRepository.findAll().getFirst();
        assertThat(response.getTargetType()).isEqualTo(ReportTargetType.USER);
        assertThat(response.getPartyId()).isEqualTo(party.getId());
        assertThat(response.getTargetUserId()).isEqualTo(host.getId());
        assertThat(response.getReasonType()).isEqualTo(ReportReasonType.PAYMENT_ISSUE);
        assertThat(response.getStatus()).isEqualTo(ReportStatus.RECEIVED);
        assertThat(savedReport.getReporter().getId()).isEqualTo(member.getId());
        assertThat(savedReport.getTargetUser().getId()).isEqualTo(host.getId());
    }

    @Test
    void 파티와_무관한_사용자가_파티를_신고하면_예외가_발생한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("무관 신고 지점"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User member = userRepository.saveAndFlush(createUser("member"));
        User stranger = userRepository.saveAndFlush(createUser("stranger"));
        Party party = createParty(store, host, member);
        setAuth(stranger);
        CreateReportRequest request = createRequest(
                ReportTargetType.PARTY,
                party.getId(),
                null,
                ReportReasonType.OTHER,
                "모집 설명과 실제 운영 방식이 달라 신고합니다."
        );

        // when
        Throwable thrown = catchThrowable(() -> reportService.createReport(stranger, request));

        // then
        assertThat(thrown)
                .isInstanceOf(BusinessException.class)
                .hasMessage("해당 파티와 관련된 사용자만 신고할 수 있습니다.");
    }

    @Test
    void 같은_대상과_사유로_다시_신고하면_예외가_발생한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("중복 신고 지점"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User member = userRepository.saveAndFlush(createUser("member"));
        Party party = createParty(store, host, member);
        setAuth(member);
        CreateReportRequest request = createRequest(
                ReportTargetType.USER,
                party.getId(),
                host.getId(),
                ReportReasonType.FRAUD_SUSPECTED,
                "같은 사유 중복 신고"
        );
        reportService.createReport(member, request);

        // when
        Throwable thrown = catchThrowable(() -> reportService.createReport(member, request));

        // then
        assertThat(thrown)
                .isInstanceOf(BusinessException.class)
                .hasMessage("같은 대상과 사유로 접수 중인 신고가 이미 있습니다.");
    }

    @Test
    void 자기_자신을_신고하면_예외가_발생한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("자기 신고 지점"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User member = userRepository.saveAndFlush(createUser("member"));
        Party party = createParty(store, host, member);
        setAuth(member);
        CreateReportRequest request = createRequest(
                ReportTargetType.USER,
                party.getId(),
                member.getId(),
                ReportReasonType.SPAM,
                "자기 자신 신고"
        );

        // when
        Throwable thrown = catchThrowable(() -> reportService.createReport(member, request));

        // then
        assertThat(thrown)
                .isInstanceOf(BusinessException.class)
                .hasMessage("자기 자신은 신고할 수 없습니다.");
    }

    @Test
    void 채팅_참여자가_아니면_채팅_신고시_예외가_발생한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("채팅 신고 지점"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User member = userRepository.saveAndFlush(createUser("member"));
        User stranger = userRepository.saveAndFlush(createUser("stranger"));
        Party party = createParty(store, host, member);
        setAuth(stranger);
        CreateReportRequest request = createRequest(
                ReportTargetType.CHAT,
                party.getId(),
                host.getId(),
                ReportReasonType.INAPPROPRIATE_CHAT,
                "채팅 신고 시도"
        );

        // when
        Throwable thrown = catchThrowable(() -> reportService.createReport(stranger, request));

        // then
        assertThat(thrown)
                .isInstanceOf(BusinessException.class)
                .hasMessage("채팅 신고는 해당 파티 채팅 참여자만 접수할 수 있습니다.");
    }

    private Party createParty(Store store, User host, User member) {
        Party party = new Party(
                "신고 테스트 파티",
                "대용량 주방세제",
                21000,
                store,
                3,
                "https://open.kakao.com/o/report",
                LocalDateTime.now().plusHours(4),
                "개",
                1,
                StorageType.ROOM_TEMPERATURE,
                PackagingType.ORIGINAL_PACKAGE,
                true,
                false,
                "기본 안내",
                null,
                null,
                null,
                null
        );
        party.acceptMember(PartyMember.joinAsHost(party, host, 1));
        party.acceptMember(PartyMember.joinAsMember(party, member, 1));
        return partyRepository.saveAndFlush(party);
    }

    private Store createStore(String name) {
        return new Store(
                name,
                "서울시 테스트로",
                LocalTime.of(10, 0),
                LocalTime.of(22, 0),
                37.5,
                127.0,
                "02-0000-0000"
        );
    }

    private User createUser(String username) {
        return new User(username, username + "@test.com", "pw", "서울", 37.5, 127.0);
    }

    private CreateReportRequest createRequest(ReportTargetType targetType,
                                              Long partyId,
                                              Long targetUserId,
                                              ReportReasonType reasonType,
                                              String memo) {
        CreateReportRequest request = new CreateReportRequest();
        ReflectionTestUtils.setField(request, "targetType", targetType);
        ReflectionTestUtils.setField(request, "partyId", partyId);
        ReflectionTestUtils.setField(request, "targetUserId", targetUserId);
        ReflectionTestUtils.setField(request, "reasonType", reasonType);
        ReflectionTestUtils.setField(request, "memo", memo);
        return request;
    }

    private void setAuth(User user) {
        CustomUserDetails principal = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
