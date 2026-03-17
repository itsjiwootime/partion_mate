package com.project.partition_mate.service;

import com.project.partition_mate.domain.OutboxEvent;
import com.project.partition_mate.domain.OutboxEventType;
import com.project.partition_mate.domain.PackagingType;
import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyCloseReason;
import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.domain.PartyStatus;
import com.project.partition_mate.domain.StorageType;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.domain.UserNotification;
import com.project.partition_mate.domain.UserNotificationType;
import com.project.partition_mate.domain.WaitingQueueEntry;
import com.project.partition_mate.domain.WaitingQueueStatus;
import com.project.partition_mate.dto.ConfirmSettlementRequest;
import com.project.partition_mate.dto.PartyDetailResponse;
import com.project.partition_mate.exception.BusinessException;
import com.project.partition_mate.repository.OutboxEventRepository;
import com.project.partition_mate.repository.PartyMemberRepository;
import com.project.partition_mate.repository.PartyRepository;
import com.project.partition_mate.repository.StoreRepository;
import com.project.partition_mate.repository.UserNotificationRepository;
import com.project.partition_mate.repository.UserRepository;
import com.project.partition_mate.repository.WaitingQueueRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
@ActiveProfiles("test")
class PartyHostCancelIntegrationTest {

    @Autowired
    private PartyService partyService;

    @Autowired
    private NotificationOutboxProcessor notificationOutboxProcessor;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private PartyMemberRepository partyMemberRepository;

    @Autowired
    private WaitingQueueRepository waitingQueueRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private UserNotificationRepository userNotificationRepository;

    @Autowired
    private StoreQueryCacheSupport storeQueryCacheSupport;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        storeQueryCacheSupport.clearAll();
        userNotificationRepository.deleteAll();
        outboxEventRepository.deleteAll();
        waitingQueueRepository.deleteAll();
        partyMemberRepository.deleteAll();
        partyRepository.deleteAll();
        storeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 호스트가_파티를_취소하면_종료사유와_알림을_기록한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("호스트 취소 지점"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User member = userRepository.saveAndFlush(createUser("member"));
        User waitingUser = userRepository.saveAndFlush(createUser("waiting"));
        Party party = createParty(store, host, member);
        waitingQueueRepository.saveAndFlush(WaitingQueueEntry.create(party, waitingUser, 1));
        setAuth(host);

        // when
        PartyDetailResponse response = partyService.closeParty(party.getId());
        int processedCount = notificationOutboxProcessor.processPendingEvents();

        // then
        Party closedParty = partyRepository.findById(party.getId()).orElseThrow();
        assertThat(response.getStatus()).isEqualTo(PartyStatus.CLOSED);
        assertThat(response.getCloseReason()).isEqualTo(PartyCloseReason.HOST_CANCELED);
        assertThat(closedParty.getPartyStatus()).isEqualTo(PartyStatus.CLOSED);
        assertThat(closedParty.getCloseReason()).isEqualTo(PartyCloseReason.HOST_CANCELED);
        assertThat(closedParty.getClosedAt()).isNotNull();
        assertThat(waitingQueueRepository.findAllByPartyAndStatusOrderByQueuedAtAsc(closedParty, WaitingQueueStatus.EXPIRED))
                .hasSize(1);

        List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
        assertThat(outboxEvents)
                .hasSize(1)
                .extracting(OutboxEvent::getEventType)
                .containsExactly(OutboxEventType.PARTY_CLOSED);
        assertThat(outboxEvents.getFirst().getPayload()).contains("HOST_CANCELED");
        assertThat(processedCount).isEqualTo(1);

        List<UserNotification> notifications = userNotificationRepository.findAll();
        assertThat(notifications)
                .extracting(UserNotification::getType)
                .containsExactlyInAnyOrder(UserNotificationType.PARTY_CLOSED, UserNotificationType.WAITING_EXPIRED);
        assertThat(notifications)
                .filteredOn(notification -> notification.getType() == UserNotificationType.PARTY_CLOSED)
                .singleElement()
                .satisfies(notification -> assertThat(notification.getMessage()).contains("파티를 호스트가 취소했습니다."));
    }

    @Test
    void 참여자가_파티를_취소하면_예외가_발생한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("권한 테스트 지점"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User member = userRepository.saveAndFlush(createUser("member"));
        Party party = createParty(store, host, member);
        setAuth(member);

        // when
        Throwable thrown = catchThrowable(() -> partyService.closeParty(party.getId()));

        // then
        assertThat(thrown)
                .isInstanceOf(BusinessException.class)
                .hasMessage("파티 종료는 호스트만 처리할 수 있습니다.");
    }

    @Test
    void 정산이_확정된_파티를_취소하면_예외가_발생한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("정산 진행 지점"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User member = userRepository.saveAndFlush(createUser("member"));
        Party party = createParty(store, host, member);
        setAuth(host);
        partyService.confirmSettlement(party.getId(), settlementRequest(28000, "행사 할인가"));

        // when
        Throwable thrown = catchThrowable(() -> partyService.closeParty(party.getId()));

        // then
        assertThat(thrown)
                .isInstanceOf(BusinessException.class)
                .hasMessage("정산, 픽업, 송금 또는 거래가 시작된 파티는 호스트 취소할 수 없습니다.");
    }

    @Test
    void 픽업일정이_확정된_파티를_취소하면_예외가_발생한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("픽업 진행 지점"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User member = userRepository.saveAndFlush(createUser("member"));
        Party party = createParty(store, host, member);
        setAuth(host);
        partyService.confirmPickupSchedule(party.getId(), pickupRequest("양재역 8번 출구", LocalDateTime.now().plusHours(5)));

        // when
        Throwable thrown = catchThrowable(() -> partyService.closeParty(party.getId()));

        // then
        assertThat(thrown)
                .isInstanceOf(BusinessException.class)
                .hasMessage("정산, 픽업, 송금 또는 거래가 시작된 파티는 호스트 취소할 수 없습니다.");
    }

    private Party createParty(Store store, User host, User member) {
        Party party = new Party(
                "취소 테스트 파티",
                "대용량 휴지",
                30000,
                store,
                3,
                "https://open.kakao.com/o/test",
                LocalDateTime.now().plusHours(8),
                "개",
                1,
                StorageType.ROOM_TEMPERATURE,
                PackagingType.ORIGINAL_PACKAGE,
                true,
                false,
                "현장 전달 예정",
                null,
                null,
                null,
                null
        );
        party.acceptMember(PartyMember.joinAsHost(party, host, 1));
        if (member != null) {
            party.acceptMember(PartyMember.joinAsMember(party, member, 1));
        }
        return partyRepository.saveAndFlush(party);
    }

    private ConfirmSettlementRequest settlementRequest(int actualTotalPrice, String receiptNote) {
        ConfirmSettlementRequest request = new ConfirmSettlementRequest();
        ReflectionTestUtils.setField(request, "actualTotalPrice", actualTotalPrice);
        ReflectionTestUtils.setField(request, "receiptNote", receiptNote);
        return request;
    }

    private com.project.partition_mate.dto.ConfirmPickupScheduleRequest pickupRequest(String pickupPlace, LocalDateTime pickupTime) {
        com.project.partition_mate.dto.ConfirmPickupScheduleRequest request = new com.project.partition_mate.dto.ConfirmPickupScheduleRequest();
        ReflectionTestUtils.setField(request, "pickupPlace", pickupPlace);
        ReflectionTestUtils.setField(request, "pickupTime", pickupTime);
        return request;
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

    private void setAuth(User user) {
        CustomUserDetails principal = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
