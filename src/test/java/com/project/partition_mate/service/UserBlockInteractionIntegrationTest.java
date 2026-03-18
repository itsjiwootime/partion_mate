package com.project.partition_mate.service;

import com.project.partition_mate.domain.PackagingType;
import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.domain.StorageType;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.ChatMessageRequest;
import com.project.partition_mate.dto.CreatePartyRequest;
import com.project.partition_mate.dto.JoinPartyRequest;
import com.project.partition_mate.exception.BusinessException;
import com.project.partition_mate.repository.ChatMessageRepository;
import com.project.partition_mate.repository.ChatReadStateRepository;
import com.project.partition_mate.repository.ChatRoomRepository;
import com.project.partition_mate.repository.PartyMemberRepository;
import com.project.partition_mate.repository.PartyRepository;
import com.project.partition_mate.repository.StoreRepository;
import com.project.partition_mate.repository.UserBlockRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class UserBlockInteractionIntegrationTest {

    @Autowired
    private PartyService partyService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserBlockService userBlockService;

    @Autowired
    private UserBlockRepository userBlockRepository;

    @Autowired
    private ChatReadStateRepository chatReadStateRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private PartyMemberRepository partyMemberRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WaitingQueueRepository waitingQueueRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        userBlockRepository.deleteAll();
        chatReadStateRepository.deleteAll();
        chatMessageRepository.deleteAll();
        chatRoomRepository.deleteAll();
        waitingQueueRepository.deleteAll();
        partyMemberRepository.deleteAll();
        partyRepository.deleteAll();
        storeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 차단관계가_있으면_파티참여가_제한된다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("차단 참여 지점"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User blockedUser = userRepository.saveAndFlush(createUser("blocked"));

        setAuth(host);
        Party party = partyService.createParty(createPartyRequest(store.getId(), "차단 참여 테스트"));
        userBlockService.blockUser(host, blockedUser.getId());
        setAuth(blockedUser);

        // when
        // then
        assertThatThrownBy(() -> partyService.joinParty(party.getId(), joinPartyRequest(1)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("차단한 사용자 또는 나를 차단한 사용자가 포함된 파티에는 참여할 수 없습니다.");
    }

    @Test
    void 차단관계가_있으면_채팅방_진입과_메시지전송이_제한된다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("차단 채팅 지점"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User member = userRepository.saveAndFlush(createUser("member"));
        Party party = createParty(store, host, member);
        chatService.initializePartyChatRoom(party, host);
        userBlockService.blockUser(host, member.getId());

        // when
        // then
        assertThatThrownBy(() -> chatService.getChatRoomDetail(party.getId(), member))
                .isInstanceOf(BusinessException.class)
                .hasMessage("차단 관계가 있는 사용자가 포함된 채팅방에는 접근할 수 없습니다.");

        assertThatThrownBy(() -> chatService.sendTextMessage(party.getId(), chatMessageRequest("메시지 전송 시도"), member))
                .isInstanceOf(BusinessException.class)
                .hasMessage("차단 관계가 있는 사용자가 포함된 채팅방에는 접근할 수 없습니다.");
    }

    @Test
    void 차단된_대기자는_자동승격되지_않고_다음_대기자가_승격된다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("차단 대기열 지점"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User joinedMember = userRepository.saveAndFlush(createUser("joined-member"));
        User fillerMember = userRepository.saveAndFlush(createUser("filler-member"));
        User blockedWaitingUser = userRepository.saveAndFlush(createUser("blocked-waiting"));
        User eligibleWaitingUser = userRepository.saveAndFlush(createUser("eligible-waiting"));
        Party party = createParty(store, host, joinedMember);
        party.acceptMember(PartyMember.joinAsMember(party, fillerMember, 1));
        partyRepository.saveAndFlush(party);

        setAuth(blockedWaitingUser);
        partyService.joinParty(party.getId(), joinPartyRequest(1));
        setAuth(eligibleWaitingUser);
        partyService.joinParty(party.getId(), joinPartyRequest(1));

        userBlockService.blockUser(host, blockedWaitingUser.getId());
        setAuth(joinedMember);

        // when
        partyService.cancelJoin(party.getId());

        // then
        Party reloaded = partyRepository.findById(party.getId()).orElseThrow();
        assertThat(waitingQueueRepository.findFirstByPartyAndUserAndStatus(
                reloaded,
                blockedWaitingUser,
                com.project.partition_mate.domain.WaitingQueueStatus.EXPIRED
        ))
                .isPresent();
        assertThat(partyMemberRepository.findByParty(reloaded))
                .extracting(pm -> pm.getUser().getId())
                .contains(host.getId(), eligibleWaitingUser.getId())
                .doesNotContain(blockedWaitingUser.getId(), joinedMember.getId());
    }

    private Party createParty(Store store, User host, User member) {
        Party party = new Party(
                "차단 테스트 파티",
                "대용량 섬유유연제",
                19000,
                store,
                3,
                "https://open.kakao.com/o/test",
                LocalDateTime.now().plusHours(6),
                "개",
                1,
                StorageType.ROOM_TEMPERATURE,
                PackagingType.ORIGINAL_PACKAGE,
                false,
                false,
                "차단 테스트용",
                null,
                null,
                null,
                null
        );
        party.acceptMember(PartyMember.joinAsHost(party, host, 1));
        party.acceptMember(PartyMember.joinAsMember(party, member, 1));
        return partyRepository.saveAndFlush(party);
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
        ReflectionTestUtils.setField(request, "guideNote", "차단 테스트용 파티");
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
