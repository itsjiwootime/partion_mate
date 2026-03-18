package com.project.partition_mate.service;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.domain.PartyStatus;
import com.project.partition_mate.domain.PackagingType;
import com.project.partition_mate.domain.StorageType;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.domain.WaitingQueueStatus;
import com.project.partition_mate.dto.JoinPartyRequest;
import com.project.partition_mate.dto.JoinPartyResponse;
import com.project.partition_mate.exception.BusinessException;
import com.project.partition_mate.repository.PartyMemberRepository;
import com.project.partition_mate.repository.PartyRepository;
import com.project.partition_mate.repository.StoreRepository;
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

import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class PartyWaitingQueueIntegrationTest {

    private static final AtomicInteger STORE_SEQUENCE = new AtomicInteger();

    @Autowired
    private PartyService partyService;

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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        waitingQueueRepository.deleteAll();
        partyMemberRepository.deleteAll();
        partyRepository.deleteAll();
        storeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 잔여_수량보다_큰_참여요청이면_대기열에_등록한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore());
        User host = userRepository.saveAndFlush(createUser("host"));
        Party party = createParty(store, host, 3, 2);
        User waitingUser = userRepository.saveAndFlush(createUser("waiter"));
        setAuth(waitingUser);

        JoinPartyRequest request = joinRequest(2);

        // when
        JoinPartyResponse response = partyService.joinParty(party.getId(), request);

        // then
        assertThat(response.getJoinStatus()).isEqualTo(com.project.partition_mate.domain.ParticipationStatus.WAITING);
        assertThat(response.getWaitingPosition()).isEqualTo(1);
        assertThat(waitingQueueRepository.findAllByPartyAndStatusOrderByQueuedAtAsc(party, WaitingQueueStatus.WAITING))
                .hasSize(1);
    }

    @Test
    void 최소_소분_단위보다_작은_요청수량으로_참여하면_예외가_발생한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore());
        User host = userRepository.saveAndFlush(createUser("host"));
        Party party = createParty(store, host, 5, 1, 2);
        User joiningUser = userRepository.saveAndFlush(createUser("joining-user"));
        setAuth(joiningUser);

        JoinPartyRequest request = joinRequest(1);

        // when
        // then
        assertThatThrownBy(() -> partyService.joinParty(party.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("요청 수량은 최소 2개 이상이어야 합니다.");
    }

    @Test
    void 참여자가_취소하면_대기열_첫번째_사용자가_자동_승격된다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore());
        User host = userRepository.saveAndFlush(createUser("host"));
        Party party = createParty(store, host, 3, 1);
        User member = userRepository.saveAndFlush(createUser("member"));
        User member2 = userRepository.saveAndFlush(createUser("member-second"));
        joinDirectly(party, member, 1);
        joinDirectly(party, member2, 1);

        User waitingFirst = userRepository.saveAndFlush(createUser("waiting-first"));
        User waitingSecond = userRepository.saveAndFlush(createUser("waiting-second"));
        enqueue(party, waitingFirst, 1);
        enqueue(party, waitingSecond, 1);

        setAuth(member);

        // when
        partyService.cancelJoin(party.getId());

        // then
        Party reloaded = partyRepository.findById(party.getId()).orElseThrow();
        assertThat(reloaded.getPartyStatus()).isEqualTo(PartyStatus.FULL);
        assertThat(partyMemberRepository.findByParty(reloaded))
                .extracting(pm -> pm.getUser().getId())
                .containsExactlyInAnyOrder(host.getId(), member2.getId(), waitingFirst.getId());
        assertThat(waitingQueueRepository.findAllByPartyAndStatusOrderByQueuedAtAsc(reloaded, WaitingQueueStatus.WAITING))
                .extracting(entry -> entry.getUser().getId())
                .containsExactly(waitingSecond.getId());
    }

    @Test
    void 첫번째_대기요청이_남은수량보다_크면_뒤의_대기자는_승격되지_않는다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore());
        User host = userRepository.saveAndFlush(createUser("host"));
        Party party = createParty(store, host, 4, 1);
        User memberA = userRepository.saveAndFlush(createUser("member-a"));
        User memberB = userRepository.saveAndFlush(createUser("member-b"));
        User memberC = userRepository.saveAndFlush(createUser("member-c"));
        joinDirectly(party, memberA, 1);
        joinDirectly(party, memberB, 1);
        joinDirectly(party, memberC, 1);

        User waitingFirst = userRepository.saveAndFlush(createUser("waiting-first"));
        User waitingSecond = userRepository.saveAndFlush(createUser("waiting-second"));
        enqueue(party, waitingFirst, 2);
        enqueue(party, waitingSecond, 1);

        setAuth(memberC);

        // when
        partyService.cancelJoin(party.getId());

        // then
        Party reloaded = partyRepository.findById(party.getId()).orElseThrow();
        assertThat(reloaded.getPartyStatus()).isEqualTo(PartyStatus.RECRUITING);
        assertThat(partyMemberRepository.findByParty(reloaded))
                .extracting(pm -> pm.getUser().getId())
                .doesNotContain(waitingFirst.getId(), waitingSecond.getId());
        assertThat(waitingQueueRepository.findAllByPartyAndStatusOrderByQueuedAtAsc(reloaded, WaitingQueueStatus.WAITING))
                .extracting(entry -> entry.getUser().getId())
                .containsExactly(waitingFirst.getId(), waitingSecond.getId());
    }

    @Test
    void 최소_소분_단위보다_작은_대기요청은_자동_승격_대신_만료된다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore());
        User host = userRepository.saveAndFlush(createUser("host"));
        Party party = createParty(store, host, 5, 1, 2);
        User memberA = userRepository.saveAndFlush(createUser("member-a"));
        User memberB = userRepository.saveAndFlush(createUser("member-b"));
        joinDirectly(party, memberA, 1);
        joinDirectly(party, memberB, 1);

        User waitingInvalid = userRepository.saveAndFlush(createUser("waiting-invalid"));
        User waitingValid = userRepository.saveAndFlush(createUser("waiting-valid"));
        enqueue(party, waitingInvalid, 1);
        enqueue(party, waitingValid, 2);

        setAuth(memberB);

        // when
        partyService.cancelJoin(party.getId());

        // then
        Party reloaded = partyRepository.findById(party.getId()).orElseThrow();
        assertThat(waitingQueueRepository.findAllByPartyAndStatusOrderByQueuedAtAsc(reloaded, WaitingQueueStatus.WAITING))
                .isEmpty();
        assertThat(waitingQueueRepository.findAll())
                .filteredOn(entry -> entry.getUser().getId().equals(waitingInvalid.getId()))
                .extracting(entry -> entry.getStatus())
                .containsExactly(WaitingQueueStatus.EXPIRED);
        assertThat(waitingQueueRepository.findAll())
                .filteredOn(entry -> entry.getUser().getId().equals(waitingValid.getId()))
                .extracting(entry -> entry.getStatus())
                .containsExactly(WaitingQueueStatus.PROMOTED);
        assertThat(partyMemberRepository.findByParty(reloaded))
                .extracting(pm -> pm.getUser().getId())
                .contains(waitingValid.getId())
                .doesNotContain(waitingInvalid.getId());
    }

    @Test
    void 호스트가_취소를_요청하면_예외가_발생한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore());
        User host = userRepository.saveAndFlush(createUser("host"));
        Party party = createParty(store, host, 3, 1);
        setAuth(host);

        // when
        // then
        assertThatThrownBy(() -> partyService.cancelJoin(party.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("호스트는 참여 취소 대신 파티 종료 흐름을 사용해야 합니다.");
    }

    private Store createStore() {
        int sequence = STORE_SEQUENCE.incrementAndGet();
        return new Store(
                "테스트 지점 " + sequence,
                "서울시",
                LocalTime.of(9, 0),
                LocalTime.of(22, 0),
                37.5,
                127.0,
                "02-0000-0000"
        );
    }

    private User createUser(String username) {
        return new User(username, username + "@test.com", "pw", "서울", 37.5, 127.0);
    }

    private Party createParty(Store store, User host, int totalQuantity, int hostRequestedQuantity) {
        Party party = new Party("테스트 파티", "테스트 상품", 30000, store, totalQuantity, "https://open.kakao.com/o/test");
        PartyMember hostMember = PartyMember.joinAsHost(party, host, hostRequestedQuantity);
        party.acceptMember(hostMember);
        return partyRepository.saveAndFlush(party);
    }

    private Party createParty(Store store, User host, int totalQuantity, int hostRequestedQuantity, int minimumShareUnit) {
        Party party = new Party(
                "테스트 파티",
                "테스트 상품",
                30000,
                store,
                totalQuantity,
                null,
                java.time.LocalDateTime.now().plusDays(1),
                "개",
                minimumShareUnit,
                StorageType.ROOM_TEMPERATURE,
                PackagingType.ORIGINAL_PACKAGE,
                false,
                false,
                null,
                null,
                null,
                null,
                null
        );
        PartyMember hostMember = PartyMember.joinAsHost(party, host, hostRequestedQuantity);
        party.acceptMember(hostMember);
        return partyRepository.saveAndFlush(party);
    }

    private void joinDirectly(Party party, User user, int requestedQuantity) {
        PartyMember partyMember = PartyMember.joinAsMember(party, user, requestedQuantity);
        party.acceptMember(partyMember);
        partyMemberRepository.saveAndFlush(partyMember);
    }

    private void enqueue(Party party, User user, int requestedQuantity) {
        waitingQueueRepository.saveAndFlush(com.project.partition_mate.domain.WaitingQueueEntry.create(party, user, requestedQuantity));
    }

    private JoinPartyRequest joinRequest(int requestedQuantity) {
        JoinPartyRequest request = new JoinPartyRequest();
        ReflectionTestUtils.setField(request, "memberRequestQuantity", requestedQuantity);
        return request;
    }

    private void setAuth(User user) {
        CustomUserDetails principal = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
