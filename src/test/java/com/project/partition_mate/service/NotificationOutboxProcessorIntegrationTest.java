package com.project.partition_mate.service;

import com.project.partition_mate.domain.OutboxEvent;
import com.project.partition_mate.domain.OutboxEventStatus;
import com.project.partition_mate.domain.OutboxEventType;
import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.domain.UserNotification;
import com.project.partition_mate.domain.UserNotificationType;
import com.project.partition_mate.repository.OutboxEventRepository;
import com.project.partition_mate.repository.PartyRepository;
import com.project.partition_mate.repository.StoreRepository;
import com.project.partition_mate.repository.UserNotificationRepository;
import com.project.partition_mate.repository.UserRepository;
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
class NotificationOutboxProcessorIntegrationTest {

    @Autowired
    private NotificationOutboxService notificationOutboxService;

    @Autowired
    private NotificationOutboxProcessor notificationOutboxProcessor;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private UserNotificationRepository userNotificationRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        userNotificationRepository.deleteAll();
        outboxEventRepository.deleteAll();
        partyRepository.deleteAll();
        storeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 참여완료_이벤트를_처리하면_알림을_생성하고_완료처리한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore());
        User user = userRepository.saveAndFlush(createUser("member"));
        Party party = partyRepository.saveAndFlush(new Party(
                "참여 알림 테스트",
                "비타민",
                12000,
                store,
                3,
                "https://open.kakao.com/o/test",
                LocalDateTime.now().plusHours(3)
        ));
        notificationOutboxService.publishJoinConfirmed(party, user);

        // when
        int processedCount = notificationOutboxProcessor.processPendingEvents(LocalDateTime.now());

        // then
        OutboxEvent event = outboxEventRepository.findAll().getFirst();
        List<UserNotification> notifications = userNotificationRepository.findAll();

        assertThat(processedCount).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PROCESSED);
        assertThat(notifications)
                .hasSize(1)
                .extracting(UserNotification::getType)
                .containsExactly(UserNotificationType.PARTY_JOIN_CONFIRMED);
    }

    @Test
    void 잘못된_payload_이벤트는_다음_시각으로_재시도된다() {
        // given
        LocalDateTime now = LocalDateTime.now();
        OutboxEvent invalidEvent = outboxEventRepository.saveAndFlush(
                OutboxEvent.create("PARTY", 1L, OutboxEventType.PARTY_JOIN_CONFIRMED, "{}", now)
        );

        // when
        int firstProcessedCount = notificationOutboxProcessor.processPendingEvents(now);
        OutboxEvent firstRetryEvent = outboxEventRepository.findById(invalidEvent.getId()).orElseThrow();
        int secondProcessedCount = notificationOutboxProcessor.processPendingEvents(now.plusSeconds(30));
        OutboxEvent secondRetryEvent = outboxEventRepository.findById(invalidEvent.getId()).orElseThrow();
        int thirdProcessedCount = notificationOutboxProcessor.processPendingEvents(now.plusMinutes(1));
        OutboxEvent thirdRetryEvent = outboxEventRepository.findById(invalidEvent.getId()).orElseThrow();

        // then
        assertThat(firstProcessedCount).isEqualTo(0);
        assertThat(firstRetryEvent.getRetryCount()).isEqualTo(1);
        assertThat(firstRetryEvent.getNextAttemptAt()).isEqualTo(now.plusMinutes(1));

        assertThat(secondProcessedCount).isEqualTo(0);
        assertThat(secondRetryEvent.getRetryCount()).isEqualTo(1);

        assertThat(thirdProcessedCount).isEqualTo(0);
        assertThat(thirdRetryEvent.getRetryCount()).isEqualTo(2);
        assertThat(thirdRetryEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(userNotificationRepository.findAll()).isEmpty();
    }

    @Test
    void 같은_outbox_이벤트를_다시_처리해도_알림은_중복생성되지_않는다() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Store store = storeRepository.saveAndFlush(createStore());
        User user = userRepository.saveAndFlush(createUser("duplicate-member"));
        Party party = partyRepository.saveAndFlush(new Party(
                "중복 알림 테스트",
                "휴지",
                18000,
                store,
                2,
                "https://open.kakao.com/o/test",
                now.plusHours(2)
        ));
        notificationOutboxService.publishJoinConfirmed(party, user);
        OutboxEvent event = outboxEventRepository.findAll().getFirst();
        LocalDateTime firstProcessTime = event.getNextAttemptAt().plusSeconds(1);

        // when
        int firstProcessedCount = notificationOutboxProcessor.processPendingEvents(firstProcessTime);
        event = outboxEventRepository.findById(event.getId()).orElseThrow();
        ReflectionTestUtils.setField(event, "status", OutboxEventStatus.PENDING);
        ReflectionTestUtils.setField(event, "nextAttemptAt", firstProcessTime.plusMinutes(1));
        ReflectionTestUtils.setField(event, "processedAt", null);
        outboxEventRepository.saveAndFlush(event);
        int secondProcessedCount = notificationOutboxProcessor.processPendingEvents(firstProcessTime.plusMinutes(1));

        // then
        assertThat(firstProcessedCount).isEqualTo(1);
        assertThat(secondProcessedCount).isEqualTo(1);
        assertThat(userNotificationRepository.findAll())
                .hasSize(1)
                .extracting(UserNotification::getType)
                .containsExactly(UserNotificationType.PARTY_JOIN_CONFIRMED);
    }

    private Store createStore() {
        return new Store(
                "알림 테스트 지점",
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
