package com.project.partition_mate.service;

import com.project.partition_mate.domain.OutboxEvent;
import com.project.partition_mate.domain.OutboxEventType;
import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyCloseReason;
import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.domain.PartyStatus;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.domain.UserNotification;
import com.project.partition_mate.domain.UserNotificationType;
import com.project.partition_mate.domain.WaitingQueueEntry;
import com.project.partition_mate.domain.WaitingQueueStatus;
import com.project.partition_mate.repository.OutboxEventRepository;
import com.project.partition_mate.repository.PartyMemberRepository;
import com.project.partition_mate.repository.PartyRepository;
import com.project.partition_mate.repository.StoreRepository;
import com.project.partition_mate.repository.UserNotificationRepository;
import com.project.partition_mate.repository.UserRepository;
import com.project.partition_mate.repository.WaitingQueueRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PartyLifecycleIntegrationTest {

    @Autowired
    private PartyLifecycleService partyLifecycleService;

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
    void 마감시간을_저장한_파티가_정원을_채우면_FULL_상태가_된다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("정원 테스트 지점"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User member = userRepository.saveAndFlush(createUser("member"));
        LocalDateTime deadline = LocalDateTime.now().plusHours(6);

        Party party = new Party("정원 테스트", "비타민", 20000, store, 2, "https://open.kakao.com/o/test", deadline);

        // when
        party.acceptMember(PartyMember.joinAsHost(party, host, 1));
        party.acceptMember(PartyMember.joinAsMember(party, member, 1));

        // then
        assertThat(party.getDeadline()).isEqualTo(deadline);
        assertThat(party.getPartyStatus()).isEqualTo(PartyStatus.FULL);
        assertThat(party.getCloseReason()).isNull();
        assertThat(party.getClosedAt()).isNull();
    }

    @Test
    void 마감시간이_지난_파티를_종료하면_CLOSED_상태와_알림_이벤트를_기록한다() {
        // given
        LocalDateTime referenceTime = LocalDateTime.now();
        Store store = storeRepository.saveAndFlush(createStore("종료 테스트 지점"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User member = userRepository.saveAndFlush(createUser("member"));
        User waitingUser = userRepository.saveAndFlush(createUser("waiting"));

        Party party = new Party("마감 테스트", "휴지", 30000, store, 3, "https://open.kakao.com/o/test", referenceTime.plusHours(1));
        party.acceptMember(PartyMember.joinAsHost(party, host, 1));
        party.acceptMember(PartyMember.joinAsMember(party, member, 1));
        party = partyRepository.saveAndFlush(party);
        ReflectionTestUtils.setField(party, "deadline", referenceTime.minusMinutes(5));
        partyRepository.saveAndFlush(party);

        waitingQueueRepository.saveAndFlush(WaitingQueueEntry.create(party, waitingUser, 1));

        // when
        int closedCount = partyLifecycleService.closeExpiredParties(referenceTime);
        int processedCount = notificationOutboxProcessor.processPendingEvents(referenceTime.plusSeconds(1));

        // then
        Party closedParty = partyRepository.findById(party.getId()).orElseThrow();
        assertThat(closedCount).isEqualTo(1);
        assertThat(closedParty.getPartyStatus()).isEqualTo(PartyStatus.CLOSED);
        assertThat(closedParty.getCloseReason()).isEqualTo(PartyCloseReason.DEADLINE_EXPIRED);
        assertThat(closedParty.getClosedAt()).isEqualTo(referenceTime);
        assertThat(waitingQueueRepository.findAllByPartyAndStatusOrderByQueuedAtAsc(closedParty, WaitingQueueStatus.EXPIRED))
                .hasSize(1);

        List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
        assertThat(outboxEvents)
                .hasSize(1)
                .extracting(OutboxEvent::getEventType)
                .containsExactly(OutboxEventType.PARTY_CLOSED);
        assertThat(processedCount).isEqualTo(1);

        List<UserNotification> notifications = userNotificationRepository.findAll();
        assertThat(notifications)
                .extracting(UserNotification::getType)
                .containsExactlyInAnyOrder(UserNotificationType.PARTY_CLOSED, UserNotificationType.PARTY_CLOSED, UserNotificationType.WAITING_EXPIRED);
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
}
