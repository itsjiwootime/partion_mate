package com.project.partition_mate.service;

import com.project.partition_mate.domain.ChatRoom;
import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.StorageType;
import com.project.partition_mate.domain.PackagingType;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.ChatMessageRequest;
import com.project.partition_mate.dto.ChatRoomDetailResponse;
import com.project.partition_mate.dto.ChatRoomSummaryResponse;
import com.project.partition_mate.dto.CreatePartyRequest;
import com.project.partition_mate.dto.JoinPartyRequest;
import com.project.partition_mate.dto.UpdatePinnedNoticeRequest;
import com.project.partition_mate.repository.ChatMessageRepository;
import com.project.partition_mate.repository.ChatReadStateRepository;
import com.project.partition_mate.repository.ChatRoomRepository;
import com.project.partition_mate.repository.PartyMemberRepository;
import com.project.partition_mate.repository.PartyRepository;
import com.project.partition_mate.repository.StoreRepository;
import com.project.partition_mate.repository.UserRepository;
import com.project.partition_mate.security.CustomUserDetails;
import jakarta.persistence.EntityNotFoundException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ChatRoomFlowIntegrationTest {

    @Autowired
    private PartyService partyService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatReadStateRepository chatReadStateRepository;

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
        chatReadStateRepository.deleteAll();
        chatMessageRepository.deleteAll();
        chatRoomRepository.deleteAll();
        partyMemberRepository.deleteAll();
        partyRepository.deleteAll();
        storeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 파티를_생성하고_참여하면_채팅방과_시스템메시지가_저장된다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("채팅 테스트 지점"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User member = userRepository.saveAndFlush(createUser("member"));

        setAuth(host);
        Party party = partyService.createParty(createPartyRequest(store.getId(), "대용량 우유 소분"));

        setAuth(member);
        partyService.joinParty(party.getId(), joinPartyRequest(1));

        // when
        ChatRoom chatRoom = chatRoomRepository.findByPartyId(party.getId()).orElseThrow();
        ChatRoomDetailResponse detailResponse = chatService.getChatRoomDetail(party.getId(), host);

        // then
        assertThat(chatRoom.getParty().getId()).isEqualTo(party.getId());
        assertThat(detailResponse.getMessages())
                .extracting(message -> message.getContent())
                .containsExactly(
                        "host님이 파티 채팅방을 열었습니다.",
                        "member님이 파티에 참여했습니다."
                );
    }

    @Test
    void 참여자만_채팅방_상세를_조회하고_읽음처리할_수_있다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("읽음 테스트 지점"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User member = userRepository.saveAndFlush(createUser("member"));
        User outsider = userRepository.saveAndFlush(createUser("outsider"));

        setAuth(host);
        Party party = partyService.createParty(createPartyRequest(store.getId(), "읽음 처리 파티"));

        setAuth(member);
        partyService.joinParty(party.getId(), joinPartyRequest(1));

        setAuth(host);
        chatService.sendTextMessage(party.getId(), chatMessageRequest("픽업은 오후 7시에 진행합니다."), host);

        // when
        ChatRoomSummaryResponse beforeRead = chatService.getMyChatRooms(member).getFirst();
        ChatRoomDetailResponse detailResponse = chatService.getChatRoomDetail(party.getId(), member);
        ChatRoomSummaryResponse afterRead = chatService.getMyChatRooms(member).getFirst();

        // then
        assertThat(beforeRead.getUnreadCount()).isGreaterThan(0);
        assertThat(detailResponse.getUnreadCount()).isZero();
        assertThat(afterRead.getUnreadCount()).isZero();

        // when & then
        assertThatThrownBy(() -> chatService.getChatRoomDetail(party.getId(), outsider))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("파티 참여자만 채팅을 사용할 수 있습니다.");
    }

    @Test
    void 호스트가_공지_고정을_변경하면_시스템메시지와_안읽은_개수가_갱신된다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("공지 테스트 지점"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User member = userRepository.saveAndFlush(createUser("member"));

        setAuth(host);
        Party party = partyService.createParty(createPartyRequest(store.getId(), "공지 고정 파티"));

        setAuth(member);
        partyService.joinParty(party.getId(), joinPartyRequest(1));

        setAuth(host);

        // when
        ChatRoomDetailResponse detailResponse = chatService.updatePinnedNotice(
                party.getId(),
                updatePinnedNoticeRequest("픽업 10분 전 도착 부탁드립니다."),
                host
        );
        ChatRoomSummaryResponse memberSummary = chatService.getMyChatRooms(member).getFirst();

        // then
        assertThat(detailResponse.getPinnedNotice()).isEqualTo("픽업 10분 전 도착 부탁드립니다.");
        assertThat(detailResponse.getMessages())
                .extracting(message -> message.getContent())
                .contains("호스트 공지가 업데이트되었습니다.");
        assertThat(memberSummary.getPinnedNotice()).isEqualTo("픽업 10분 전 도착 부탁드립니다.");
        assertThat(memberSummary.getUnreadCount()).isGreaterThan(0);
    }

    private CreatePartyRequest createPartyRequest(Long storeId, String title) {
        CreatePartyRequest request = new CreatePartyRequest();
        ReflectionTestUtils.setField(request, "title", title);
        ReflectionTestUtils.setField(request, "storeId", storeId);
        ReflectionTestUtils.setField(request, "productName", "대용량 두유");
        ReflectionTestUtils.setField(request, "totalPrice", 18000);
        ReflectionTestUtils.setField(request, "totalQuantity", 3);
        ReflectionTestUtils.setField(request, "hostRequestedQuantity", 1);
        ReflectionTestUtils.setField(request, "deadline", LocalDateTime.now().plusHours(8));
        ReflectionTestUtils.setField(request, "unitLabel", "개");
        ReflectionTestUtils.setField(request, "minimumShareUnit", 1);
        ReflectionTestUtils.setField(request, "storageType", StorageType.ROOM_TEMPERATURE);
        ReflectionTestUtils.setField(request, "packagingType", PackagingType.ORIGINAL_PACKAGE);
        ReflectionTestUtils.setField(request, "hostProvidesPackaging", false);
        ReflectionTestUtils.setField(request, "onSiteSplit", false);
        ReflectionTestUtils.setField(request, "guideNote", "채팅 테스트용 파티");
        return request;
    }

    private JoinPartyRequest joinPartyRequest(int quantity) {
        JoinPartyRequest request = new JoinPartyRequest();
        ReflectionTestUtils.setField(request, "memberRequestQuantity", quantity);
        return request;
    }

    private ChatMessageRequest chatMessageRequest(String content) {
        ChatMessageRequest request = new ChatMessageRequest();
        ReflectionTestUtils.setField(request, "content", content);
        return request;
    }

    private UpdatePinnedNoticeRequest updatePinnedNoticeRequest(String pinnedNotice) {
        UpdatePinnedNoticeRequest request = new UpdatePinnedNoticeRequest();
        ReflectionTestUtils.setField(request, "pinnedNotice", pinnedNotice);
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
