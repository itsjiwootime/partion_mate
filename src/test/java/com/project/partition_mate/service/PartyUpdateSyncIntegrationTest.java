package com.project.partition_mate.service;

import com.project.partition_mate.domain.ChatMessage;
import com.project.partition_mate.domain.ChatRoom;
import com.project.partition_mate.domain.OutboxEvent;
import com.project.partition_mate.domain.OutboxEventType;
import com.project.partition_mate.domain.PackagingType;
import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.domain.PartyStatus;
import com.project.partition_mate.domain.StorageType;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.domain.UserNotification;
import com.project.partition_mate.domain.UserNotificationType;
import com.project.partition_mate.domain.WaitingQueueEntry;
import com.project.partition_mate.domain.WaitingQueueStatus;
import com.project.partition_mate.dto.PartyDetailResponse;
import com.project.partition_mate.dto.UpdatePartyRequest;
import com.project.partition_mate.repository.ChatMessageRepository;
import com.project.partition_mate.repository.ChatRoomRepository;
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

@SpringBootTest
@ActiveProfiles("test")
class PartyUpdateSyncIntegrationTest {

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
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        userNotificationRepository.deleteAll();
        outboxEventRepository.deleteAll();
        waitingQueueRepository.deleteAll();
        partyMemberRepository.deleteAll();
        partyRepository.deleteAll();
        storeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 호스트가_파티를_수정하면_알림과_채팅과_대기열을_함께_동기화한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("조건 변경 지점"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User member = userRepository.saveAndFlush(createUser("member"));
        User waitingUser = userRepository.saveAndFlush(createUser("waiting"));
        Party party = createFullParty(store, host, member);
        waitingQueueRepository.saveAndFlush(WaitingQueueEntry.create(party, waitingUser, 1));
        setAuth(host);
        UpdatePartyRequest request = updateRequest(
                party.getTitle(),
                party.getProductName(),
                party.getExpectedTotalPrice(),
                3,
                party.getOpenChatUrl(),
                LocalDateTime.now().plusHours(10),
                party.getUnitLabel(),
                party.getMinimumShareUnit(),
                party.getStorageType(),
                party.getPackagingType(),
                party.isHostProvidesPackaging(),
                party.isOnSiteSplit(),
                "픽업 15분 전까지 도착해 주세요."
        );

        // when
        PartyDetailResponse response = partyService.updateParty(party.getId(), request);
        int processedCount = notificationOutboxProcessor.processPendingEvents();

        // then
        Party updatedParty = partyRepository.findDetailById(party.getId()).orElseThrow();
        assertThat(response.getStatus()).isEqualTo(PartyStatus.FULL);
        assertThat(updatedParty.getPartyStatus()).isEqualTo(PartyStatus.FULL);
        assertThat(updatedParty.getGuideNote()).isEqualTo("픽업 15분 전까지 도착해 주세요.");
        assertThat(waitingQueueRepository.findAllByPartyAndStatusOrderByQueuedAtAsc(updatedParty, WaitingQueueStatus.PROMOTED))
                .hasSize(1);

        List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
        assertThat(outboxEvents)
                .extracting(OutboxEvent::getEventType)
                .containsExactlyInAnyOrder(OutboxEventType.WAITING_PROMOTED, OutboxEventType.PARTY_UPDATED);
        assertThat(outboxEvents)
                .filteredOn(event -> event.getEventType() == OutboxEventType.PARTY_UPDATED)
                .singleElement()
                .satisfies(event -> assertThat(event.getPayload()).contains("변경 항목: 총 수량, 마감 시간, 안내 문구"));
        assertThat(processedCount).isEqualTo(2);

        List<UserNotification> notifications = userNotificationRepository.findAll();
        assertThat(notifications)
                .extracting(UserNotification::getType)
                .containsExactlyInAnyOrder(UserNotificationType.WAITING_PROMOTED, UserNotificationType.PARTY_UPDATED);
        assertThat(notifications)
                .filteredOn(notification -> notification.getType() == UserNotificationType.PARTY_UPDATED)
                .singleElement()
                .satisfies(notification -> assertThat(notification.getMessage()).contains("변경 항목: 총 수량, 마감 시간, 안내 문구"));

        ChatRoom chatRoom = chatRoomRepository.findByPartyId(party.getId()).orElseThrow();
        List<ChatMessage> messages = chatMessageRepository.findTop100ByChatRoomOrderByIdDesc(chatRoom);
        assertThat(messages)
                .extracting(ChatMessage::getContent)
                .anySatisfy(content -> assertThat(content).contains("호스트가 파티 조건을 수정했습니다."))
                .anySatisfy(content -> assertThat(content).contains("대기열에서 채팅방 참여 상태로 승격되었습니다."));
    }

    private Party createFullParty(Store store, User host, User member) {
        Party party = new Party(
                "변경 테스트 파티",
                "대용량 견과류",
                32000,
                store,
                2,
                "https://open.kakao.com/o/update-test",
                LocalDateTime.now().plusHours(6),
                "봉",
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

    private UpdatePartyRequest updateRequest(String title,
                                             String productName,
                                             Integer totalPrice,
                                             Integer totalQuantity,
                                             String openChatUrl,
                                             LocalDateTime deadline,
                                             String unitLabel,
                                             Integer minimumShareUnit,
                                             StorageType storageType,
                                             PackagingType packagingType,
                                             boolean hostProvidesPackaging,
                                             boolean onSiteSplit,
                                             String guideNote) {
        UpdatePartyRequest request = new UpdatePartyRequest();
        ReflectionTestUtils.setField(request, "title", title);
        ReflectionTestUtils.setField(request, "productName", productName);
        ReflectionTestUtils.setField(request, "totalPrice", totalPrice);
        ReflectionTestUtils.setField(request, "totalQuantity", totalQuantity);
        ReflectionTestUtils.setField(request, "openChatUrl", openChatUrl);
        ReflectionTestUtils.setField(request, "deadline", deadline);
        ReflectionTestUtils.setField(request, "unitLabel", unitLabel);
        ReflectionTestUtils.setField(request, "minimumShareUnit", minimumShareUnit);
        ReflectionTestUtils.setField(request, "storageType", storageType);
        ReflectionTestUtils.setField(request, "packagingType", packagingType);
        ReflectionTestUtils.setField(request, "hostProvidesPackaging", hostProvidesPackaging);
        ReflectionTestUtils.setField(request, "onSiteSplit", onSiteSplit);
        ReflectionTestUtils.setField(request, "guideNote", guideNote);
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
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
