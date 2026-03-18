package com.project.partition_mate.service;

import com.project.partition_mate.domain.ExternalNotificationDelivery;
import com.project.partition_mate.domain.ExternalNotificationDeliveryStatus;
import com.project.partition_mate.domain.OutboxEvent;
import com.project.partition_mate.domain.OutboxEventType;
import com.project.partition_mate.domain.PackagingType;
import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.domain.StorageType;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.domain.UserNotification;
import com.project.partition_mate.domain.UserNotificationPreference;
import com.project.partition_mate.domain.UserNotificationType;
import com.project.partition_mate.domain.WebPushSubscription;
import com.project.partition_mate.dto.ConfirmPickupScheduleRequest;
import com.project.partition_mate.repository.OutboxEventRepository;
import com.project.partition_mate.repository.ExternalNotificationDeliveryRepository;
import com.project.partition_mate.repository.PartyMemberRepository;
import com.project.partition_mate.repository.PartyRepository;
import com.project.partition_mate.repository.StoreRepository;
import com.project.partition_mate.repository.UserNotificationRepository;
import com.project.partition_mate.repository.UserNotificationPreferenceRepository;
import com.project.partition_mate.repository.UserRepository;
import com.project.partition_mate.repository.WebPushSubscriptionRepository;
import com.project.partition_mate.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(NotificationOutboxWebPushIntegrationTest.TestConfig.class)
@ActiveProfiles("test")
@TestExecutionListeners(
        listeners = {
                DependencyInjectionTestExecutionListener.class,
                DirtiesContextBeforeModesTestExecutionListener.class,
                DirtiesContextTestExecutionListener.class
        },
        mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS
)
class NotificationOutboxWebPushIntegrationTest {

    @Autowired
    private NotificationOutboxService notificationOutboxService;

    @Autowired
    private NotificationOutboxProcessor notificationOutboxProcessor;

    @Autowired
    private ExternalNotificationDeliveryService externalNotificationDeliveryService;

    @Autowired
    private WebPushSubscriptionRepository webPushSubscriptionRepository;

    @Autowired
    private RecordingWebPushGateway recordingWebPushGateway;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ExternalNotificationDeliveryRepository externalNotificationDeliveryRepository;

    @Autowired
    private UserNotificationRepository userNotificationRepository;

    @Autowired
    private UserNotificationPreferenceRepository userNotificationPreferenceRepository;

    @Autowired
    private PartyService partyService;

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
        recordingWebPushGateway.reset();
        externalNotificationDeliveryRepository.deleteAll();
        userNotificationRepository.deleteAll();
        outboxEventRepository.deleteAll();
        userNotificationPreferenceRepository.deleteAll();
        webPushSubscriptionRepository.deleteAll();
        partyMemberRepository.deleteAll();
        partyRepository.deleteAll();
        storeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 대기열_승격_이벤트를_처리하면_앱내알림과_web_push를_함께_보낸다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("승격 지점"));
        User user = userRepository.saveAndFlush(createUser("member"));
        Party party = partyRepository.saveAndFlush(new Party(
                "승격 테스트",
                "휴지",
                20000,
                store,
                2,
                "https://open.kakao.com/o/push",
                LocalDateTime.now().plusHours(3)
        ));
        saveSubscription(user, "https://push.example.com/subscriptions/promoted");
        notificationOutboxService.publishWaitingPromoted(party, user, 1);
        LocalDateTime processingTime = outboxEventRepository.findAll().getFirst().getNextAttemptAt().plusSeconds(1);

        // when
        int processedCount = notificationOutboxProcessor.processPendingEvents(processingTime);
        int deliveryProcessedCount = externalNotificationDeliveryService.processPendingDeliveries(processingTime);

        // then
        assertThat(processedCount).isEqualTo(1);
        assertThat(deliveryProcessedCount).isEqualTo(1);
        assertThat(userNotificationRepository.findAll())
                .extracting(UserNotification::getType)
                .containsExactly(UserNotificationType.WAITING_PROMOTED);
        assertThat(externalNotificationDeliveryRepository.findAll())
                .extracting(ExternalNotificationDelivery::getStatus)
                .containsExactly(ExternalNotificationDeliveryStatus.SENT);
        assertThat(recordingWebPushGateway.attempts).hasSize(1);
        assertThat(recordingWebPushGateway.attempts.getFirst().payloadJson()).contains("\"type\":\"WAITING_PROMOTED\"");
        assertThat(recordingWebPushGateway.attempts.getFirst().payloadJson()).contains("\"url\":\"/chat/" + party.getId() + "\"");
    }

    @Test
    void 픽업일정이_확정되면_참여자에게_pickup_updated_알림과_web_push를_보낸다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("픽업 지점"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User member = userRepository.saveAndFlush(createUser("member"));
        Party party = createParty(store, host, member);
        saveSubscription(member, "https://push.example.com/subscriptions/pickup");
        setAuth(host);

        // when
        partyService.confirmPickupSchedule(party.getId(), pickupRequest("강남역 1번 출구", LocalDateTime.now().plusHours(5)));
        LocalDateTime processingTime = outboxEventRepository.findAll().getFirst().getNextAttemptAt().plusSeconds(1);
        int processedCount = notificationOutboxProcessor.processPendingEvents(processingTime);
        int deliveryProcessedCount = externalNotificationDeliveryService.processPendingDeliveries(processingTime);

        // then
        assertThat(processedCount).isEqualTo(1);
        assertThat(deliveryProcessedCount).isEqualTo(1);
        assertThat(outboxEventRepository.findAll())
                .extracting(OutboxEvent::getEventType)
                .containsExactly(OutboxEventType.PICKUP_UPDATED);
        assertThat(userNotificationRepository.findAll())
                .extracting(UserNotification::getType)
                .containsExactly(UserNotificationType.PICKUP_UPDATED);
        assertThat(recordingWebPushGateway.attempts).hasSize(1);
        assertThat(recordingWebPushGateway.attempts.getFirst().payloadJson()).contains("\"type\":\"PICKUP_UPDATED\"");
    }

    @Test
    void 만료된_구독은_410_응답을_받으면_삭제한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("만료 구독 지점"));
        User user = userRepository.saveAndFlush(createUser("member"));
        Party party = partyRepository.saveAndFlush(new Party(
                "만료 구독 테스트",
                "세제",
                15000,
                store,
                2,
                "https://open.kakao.com/o/push",
                LocalDateTime.now().plusHours(2)
        ));
        String endpoint = "https://push.example.com/subscriptions/gone";
        saveSubscription(user, endpoint);
        recordingWebPushGateway.withStatus(endpoint, 410);
        notificationOutboxService.publishWaitingPromoted(party, user, 1);
        LocalDateTime processingTime = outboxEventRepository.findAll().getFirst().getNextAttemptAt().plusSeconds(1);

        // when
        notificationOutboxProcessor.processPendingEvents(processingTime);
        externalNotificationDeliveryService.processPendingDeliveries(processingTime);

        // then
        assertThat(webPushSubscriptionRepository.findAll()).isEmpty();
        assertThat(externalNotificationDeliveryRepository.findAll())
                .extracting(ExternalNotificationDelivery::getStatus)
                .containsExactly(ExternalNotificationDeliveryStatus.FAILED);
    }

    @Test
    void 웹푸시_설정이_off면_앱내알림만_저장하고_web_push는_보내지_않는다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("설정 off 지점"));
        User user = userRepository.saveAndFlush(createUser("member"));
        Party party = partyRepository.saveAndFlush(new Party(
                "설정 off 테스트",
                "세제",
                15000,
                store,
                2,
                "https://open.kakao.com/o/push",
                LocalDateTime.now().plusHours(2)
        ));
        saveSubscription(user, "https://push.example.com/subscriptions/off");
        userNotificationPreferenceRepository.saveAndFlush(
                UserNotificationPreference.create(user, UserNotificationType.WAITING_PROMOTED, false, LocalDateTime.now())
        );
        notificationOutboxService.publishWaitingPromoted(party, user, 1);
        LocalDateTime processingTime = outboxEventRepository.findAll().getFirst().getNextAttemptAt().plusSeconds(1);

        // when
        notificationOutboxProcessor.processPendingEvents(processingTime);
        int deliveryProcessedCount = externalNotificationDeliveryService.processPendingDeliveries(processingTime);

        // then
        assertThat(userNotificationRepository.findAll())
                .extracting(UserNotification::getType)
                .containsExactly(UserNotificationType.WAITING_PROMOTED);
        assertThat(deliveryProcessedCount).isEqualTo(0);
        assertThat(recordingWebPushGateway.attempts).isEmpty();
        assertThat(externalNotificationDeliveryRepository.findAll()).isEmpty();
    }

    @Test
    void 외부발송이_실패하면_앱내알림은_유지되고_재시도후_failed로_남는다() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Store store = storeRepository.saveAndFlush(createStore("재시도 지점"));
        User user = userRepository.saveAndFlush(createUser("member"));
        Party party = partyRepository.saveAndFlush(new Party(
                "재시도 테스트",
                "세제",
                15000,
                store,
                2,
                "https://open.kakao.com/o/push",
                now.plusHours(2)
        ));
        String endpoint = "https://push.example.com/subscriptions/retry";
        saveSubscription(user, endpoint);
        recordingWebPushGateway.withStatus(endpoint, 503);
        notificationOutboxService.publishWaitingPromoted(party, user, 1);
        LocalDateTime processingTime = outboxEventRepository.findAll().getFirst().getNextAttemptAt().plusSeconds(1);

        // when
        notificationOutboxProcessor.processPendingEvents(processingTime);
        int firstDeliveryCount = externalNotificationDeliveryService.processPendingDeliveries(processingTime);
        int secondDeliveryCount = externalNotificationDeliveryService.processPendingDeliveries(processingTime.plusMinutes(1));
        int thirdDeliveryCount = externalNotificationDeliveryService.processPendingDeliveries(processingTime.plusMinutes(6));

        // then
        assertThat(firstDeliveryCount).isEqualTo(1);
        assertThat(secondDeliveryCount).isEqualTo(1);
        assertThat(thirdDeliveryCount).isEqualTo(1);
        assertThat(userNotificationRepository.findAll())
                .extracting(UserNotification::getType)
                .containsExactly(UserNotificationType.WAITING_PROMOTED);
        assertThat(outboxEventRepository.findAll())
                .extracting(OutboxEvent::getStatus)
                .containsExactly(com.project.partition_mate.domain.OutboxEventStatus.PROCESSED);
        assertThat(externalNotificationDeliveryRepository.findAll())
                .hasSize(1)
                .extracting(ExternalNotificationDelivery::getStatus)
                .containsExactly(ExternalNotificationDeliveryStatus.FAILED);
        assertThat(recordingWebPushGateway.attempts).hasSize(3);
    }

    @Test
    void 같은_outbox를_다시_처리해도_external_delivery_key로_중복생성을_막는다() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Store store = storeRepository.saveAndFlush(createStore("중복 방지 지점"));
        User user = userRepository.saveAndFlush(createUser("member"));
        Party party = partyRepository.saveAndFlush(new Party(
                "중복 방지 테스트",
                "휴지",
                20000,
                store,
                2,
                "https://open.kakao.com/o/push",
                now.plusHours(3)
        ));
        saveSubscription(user, "https://push.example.com/subscriptions/dedup");
        notificationOutboxService.publishWaitingPromoted(party, user, 1);
        OutboxEvent event = outboxEventRepository.findAll().getFirst();
        LocalDateTime processingTime = event.getNextAttemptAt().plusSeconds(1);

        // when
        notificationOutboxProcessor.processPendingEvents(processingTime);
        event = outboxEventRepository.findById(event.getId()).orElseThrow();
        ReflectionTestUtils.setField(event, "status", com.project.partition_mate.domain.OutboxEventStatus.PENDING);
        ReflectionTestUtils.setField(event, "nextAttemptAt", processingTime.plusMinutes(1));
        ReflectionTestUtils.setField(event, "processedAt", null);
        outboxEventRepository.saveAndFlush(event);
        notificationOutboxProcessor.processPendingEvents(processingTime.plusMinutes(1));

        // then
        assertThat(externalNotificationDeliveryRepository.findAll()).hasSize(1);
    }

    private Store createStore(String name) {
        return new Store(
                name,
                "서울시",
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

    private Party createParty(Store store, User host, User member) {
        Party party = new Party(
                "픽업 알림 테스트 파티",
                "대용량 휴지",
                30000,
                store,
                2,
                "https://open.kakao.com/o/pickup",
                LocalDateTime.now().plusHours(6),
                "개",
                1,
                StorageType.ROOM_TEMPERATURE,
                PackagingType.ORIGINAL_PACKAGE,
                true,
                false,
                "현장 전달",
                null,
                null,
                null,
                null
        );
        party.acceptMember(PartyMember.joinAsHost(party, host, 1));
        party.acceptMember(PartyMember.joinAsMember(party, member, 1));
        return partyRepository.saveAndFlush(party);
    }

    private void saveSubscription(User user, String endpoint) {
        webPushSubscriptionRepository.saveAndFlush(
                WebPushSubscription.create(
                        user,
                        endpoint,
                        "p256dh-key",
                        "auth-key",
                        "Chrome",
                        LocalDateTime.now()
                )
        );
    }

    private ConfirmPickupScheduleRequest pickupRequest(String pickupPlace, LocalDateTime pickupTime) {
        ConfirmPickupScheduleRequest request = new ConfirmPickupScheduleRequest();
        ReflectionTestUtils.setField(request, "pickupPlace", pickupPlace);
        ReflectionTestUtils.setField(request, "pickupTime", pickupTime);
        return request;
    }

    private void setAuth(User user) {
        CustomUserDetails principal = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        RecordingWebPushGateway recordingWebPushGateway() {
            return new RecordingWebPushGateway();
        }
    }

    static class RecordingWebPushGateway implements WebPushGateway {

        private final List<Attempt> attempts = new ArrayList<>();
        private final Map<String, Integer> statusByEndpoint = new HashMap<>();

        @Override
        public DeliveryResult send(WebPushSubscription subscription, String payloadJson) {
            attempts.add(new Attempt(subscription.getEndpoint(), payloadJson));
            return new DeliveryResult(statusByEndpoint.getOrDefault(subscription.getEndpoint(), 201));
        }

        void withStatus(String endpoint, int statusCode) {
            statusByEndpoint.put(endpoint, statusCode);
        }

        void reset() {
            attempts.clear();
            statusByEndpoint.clear();
        }

        record Attempt(String endpoint, String payloadJson) {
        }
    }
}
