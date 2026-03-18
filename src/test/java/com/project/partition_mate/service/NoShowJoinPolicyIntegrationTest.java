package com.project.partition_mate.service;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.domain.ParticipationStatus;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.domain.TradeStatus;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.JoinPartyRequest;
import com.project.partition_mate.dto.JoinPartyResponse;
import com.project.partition_mate.exception.BusinessException;
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
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
@ActiveProfiles("test")
@TestExecutionListeners(
        listeners = {
                DependencyInjectionTestExecutionListener.class,
                DirtiesContextBeforeModesTestExecutionListener.class,
                DirtiesContextTestExecutionListener.class
        },
        mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS
)
class NoShowJoinPolicyIntegrationTest {

    private static final AtomicInteger STORE_SEQUENCE = new AtomicInteger();
    private static final AtomicInteger PARTY_SEQUENCE = new AtomicInteger();

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
        partyMemberRepository.deleteAll();
        partyRepository.deleteAll();
        storeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 최근_노쇼가_1회면_경고와_함께_파티에_참여한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore());
        User historyHost = userRepository.saveAndFlush(createUser("history-host"));
        User targetUser = userRepository.saveAndFlush(createUser("warn-user"));
        createTradeHistory(store, historyHost, targetUser, TradeStatus.NO_SHOW, LocalDateTime.now().minusDays(1));

        User partyHost = userRepository.saveAndFlush(createUser("party-host"));
        Party targetParty = createRecruitingParty(store, partyHost, 2, 1);
        setAuth(targetUser);

        // when
        JoinPartyResponse response = partyService.joinParty(targetParty.getId(), joinRequest(1));

        // then
        assertThat(response.getJoinStatus()).isEqualTo(ParticipationStatus.JOINED);
        assertThat(response.getWarning()).isNotNull();
        assertThat(response.getWarning().getCode()).isEqualTo("NO_SHOW_CAUTION");
        assertThat(response.getWarning().getActiveNoShowCount()).isEqualTo(1);
    }

    @Test
    void 최근_노쇼가_2회_연속이면_파티_참여를_차단한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore());
        User historyHost = userRepository.saveAndFlush(createUser("history-host"));
        User targetUser = userRepository.saveAndFlush(createUser("blocked-user"));
        createTradeHistory(store, historyHost, targetUser, TradeStatus.NO_SHOW, LocalDateTime.now().minusDays(2));
        createTradeHistory(store, historyHost, targetUser, TradeStatus.NO_SHOW, LocalDateTime.now().minusDays(1));

        User partyHost = userRepository.saveAndFlush(createUser("party-host"));
        Party targetParty = createRecruitingParty(store, partyHost, 2, 1);
        setAuth(targetUser);

        // when
        Throwable thrown = catchThrowable(() -> partyService.joinParty(targetParty.getId(), joinRequest(1)));

        // then
        assertThat(thrown)
                .isInstanceOf(BusinessException.class)
                .hasMessage("최근 노쇼가 2회 연속 기록되어 새 거래를 1회 완료하기 전까지 파티에 참여할 수 없습니다.");
    }

    @Test
    void 최근_노쇼_이후에_완료거래가_있으면_참여제한을_해제한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore());
        User historyHost = userRepository.saveAndFlush(createUser("history-host"));
        User targetUser = userRepository.saveAndFlush(createUser("recovered-user"));
        createTradeHistory(store, historyHost, targetUser, TradeStatus.NO_SHOW, LocalDateTime.now().minusDays(3));
        createTradeHistory(store, historyHost, targetUser, TradeStatus.NO_SHOW, LocalDateTime.now().minusDays(2));
        createTradeHistory(store, historyHost, targetUser, TradeStatus.COMPLETED, LocalDateTime.now().minusDays(1));

        User partyHost = userRepository.saveAndFlush(createUser("party-host"));
        Party targetParty = createRecruitingParty(store, partyHost, 2, 1);
        setAuth(targetUser);

        // when
        JoinPartyResponse response = partyService.joinParty(targetParty.getId(), joinRequest(1));

        // then
        assertThat(response.getJoinStatus()).isEqualTo(ParticipationStatus.JOINED);
        assertThat(response.getWarning()).isNull();
    }

    private Store createStore() {
        int sequence = STORE_SEQUENCE.incrementAndGet();
        return new Store(
                "노쇼 정책 지점 " + sequence,
                "서울시",
                LocalTime.of(9, 0),
                LocalTime.of(22, 0),
                37.5,
                127.0,
                "02-0000-0000"
        );
    }

    private User createUser(String usernamePrefix) {
        int sequence = PARTY_SEQUENCE.incrementAndGet();
        String username = usernamePrefix + "-" + sequence;
        return new User(username, username + "@test.com", "pw", "서울", 37.5, 127.0);
    }

    private Party createRecruitingParty(Store store, User host, int totalQuantity, int hostRequestedQuantity) {
        Party party = new Party(
                "노쇼 정책 대상 파티 " + PARTY_SEQUENCE.incrementAndGet(),
                "테스트 상품",
                30000,
                store,
                totalQuantity,
                "https://open.kakao.com/o/no-show-policy"
        );
        party.acceptMember(PartyMember.joinAsHost(party, host, hostRequestedQuantity));
        return partyRepository.saveAndFlush(party);
    }

    private void createTradeHistory(Store store,
                                    User host,
                                    User member,
                                    TradeStatus tradeStatus,
                                    LocalDateTime tradeStatusUpdatedAt) {
        Party party = new Party(
                "노쇼 이력 파티 " + PARTY_SEQUENCE.incrementAndGet(),
                "이력 상품",
                21000,
                store,
                2,
                "https://open.kakao.com/o/no-show-history"
        );
        PartyMember hostMember = PartyMember.joinAsHost(party, host, 1);
        PartyMember memberEntry = PartyMember.joinAsMember(party, member, 1);
        party.acceptMember(hostMember);
        party.acceptMember(memberEntry);
        if (tradeStatus == TradeStatus.COMPLETED) {
            memberEntry.completeTrade(tradeStatusUpdatedAt);
        } else {
            memberEntry.markNoShow(tradeStatusUpdatedAt);
        }
        partyRepository.saveAndFlush(party);
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
