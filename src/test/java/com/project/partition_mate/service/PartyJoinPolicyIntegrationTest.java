package com.project.partition_mate.service;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.domain.PartyStatus;
import com.project.partition_mate.domain.PackagingType;
import com.project.partition_mate.domain.StorageType;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.JoinPartyRequest;
import com.project.partition_mate.exception.BusinessException;
import com.project.partition_mate.repository.OutboxEventRepository;
import com.project.partition_mate.repository.PartyMemberRepository;
import com.project.partition_mate.repository.PartyRepository;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class PartyJoinPolicyIntegrationTest {

    private static final AtomicInteger STORE_SEQUENCE = new AtomicInteger();

    @Autowired
    private PartyService partyService;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private PartyMemberRepository partyMemberRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        outboxEventRepository.deleteAll();
        partyMemberRepository.deleteAll();
        partyRepository.deleteAll();
        storeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 잔여_수량보다_큰_참여요청이면_예외가_발생한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore());
        User host = userRepository.saveAndFlush(createUser("host"));
        Party party = createParty(store, host, 3, 2);
        User joiningUser = userRepository.saveAndFlush(createUser("joining-user"));
        setAuth(joiningUser);

        JoinPartyRequest request = joinRequest(2);

        // when
        // then
        assertThatThrownBy(() -> partyService.joinParty(party.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("남은 수량(1개)이 부족하여 참여할 수 없습니다.");
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
    void 참여자가_취소하면_남은_수량이_복구된다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore());
        User host = userRepository.saveAndFlush(createUser("host"));
        Party party = createParty(store, host, 3, 1);
        User member = userRepository.saveAndFlush(createUser("member"));
        joinDirectly(party, member, 1);
        setAuth(member);

        // when
        partyService.cancelJoin(party.getId());

        // then
        Party reloaded = partyRepository.findById(party.getId()).orElseThrow();
        var joinedMembers = partyMemberRepository.findByParty(reloaded);
        assertThat(reloaded.getPartyStatus()).isEqualTo(PartyStatus.RECRUITING);
        assertThat(joinedMembers.stream().mapToInt(PartyMember::getRequestedQuantity).sum()).isEqualTo(1);
        assertThat(joinedMembers)
                .extracting(pm -> pm.getUser().getId())
                .containsExactly(host.getId());
    }

    @Test
    void 참여하지_않은_사용자가_취소를_요청하면_예외가_발생한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore());
        User host = userRepository.saveAndFlush(createUser("host"));
        Party party = createParty(store, host, 3, 1);
        User stranger = userRepository.saveAndFlush(createUser("stranger"));
        setAuth(stranger);

        // when
        // then
        assertThatThrownBy(() -> partyService.cancelJoin(party.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("참여 중인 내역이 없습니다.");
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
        return createParty(store, host, totalQuantity, hostRequestedQuantity, 1);
    }

    private Party createParty(Store store, User host, int totalQuantity, int hostRequestedQuantity, int minimumShareUnit) {
        Party party = new Party(
                "정책 테스트 파티",
                "대용량 상품",
                30000,
                store,
                totalQuantity,
                "https://open.kakao.com/o/test",
                LocalDateTime.now().plusHours(6),
                "개",
                minimumShareUnit,
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
        party.acceptMember(PartyMember.joinAsHost(party, host, hostRequestedQuantity));
        return partyRepository.saveAndFlush(party);
    }

    private void joinDirectly(Party party, User user, int requestedQuantity) {
        PartyMember member = PartyMember.joinAsMember(party, user, requestedQuantity);
        party.acceptMember(member);
        partyMemberRepository.saveAndFlush(member);
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
