package com.project.partition_mate.service;

import com.project.partition_mate.domain.PackagingType;
import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.domain.PaymentStatus;
import com.project.partition_mate.domain.StorageType;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.domain.TradeStatus;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.ConfirmPickupScheduleRequest;
import com.project.partition_mate.dto.ConfirmSettlementRequest;
import com.project.partition_mate.dto.PartyDetailResponse;
import com.project.partition_mate.dto.UpdatePaymentStatusRequest;
import com.project.partition_mate.dto.UpdateTradeStatusRequest;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PartySettlementFlowIntegrationTest {

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
    void 호스트가_정산을_확정하면_참여자별_예상금액과_확정금액이_계산된다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("정산 테스트 지점"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User memberA = userRepository.saveAndFlush(createUser("member-a"));
        User memberB = userRepository.saveAndFlush(createUser("member-b"));
        Party party = createParty(store, host, memberA, memberB);
        setAuth(host);
        ConfirmSettlementRequest request = settlementRequest(28005, "행사 카드 할인 적용");

        // when
        PartyDetailResponse response = partyService.confirmSettlement(party.getId(), request);

        // then
        assertThat(response.getActualTotalPrice()).isEqualTo(28005);
        assertThat(response.getSettlementMembers()).hasSize(3);
        assertThat(response.getSettlementMembers())
                .filteredOn(member -> member.getUsername().equals("member-b"))
                .singleElement()
                .satisfies(member -> {
                    assertThat(member.getExpectedAmount()).isEqualTo(15002);
                    assertThat(member.getActualAmount()).isEqualTo(14003);
                    assertThat(member.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
                });
        assertThat(response.getSettlementMembers())
                .filteredOn(member -> member.getUsername().equals("host"))
                .singleElement()
                .satisfies(member -> assertThat(member.getPaymentStatus()).isEqualTo(PaymentStatus.NOT_REQUIRED));
    }

    @Test
    void 참여자가_송금완료를_표시하고_호스트가_확인과_환불을_처리한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("송금 테스트 지점"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User memberA = userRepository.saveAndFlush(createUser("member-a"));
        User memberB = userRepository.saveAndFlush(createUser("member-b"));
        Party party = createParty(store, host, memberA, memberB);
        confirmSettlementAsHost(party, host, 28005);

        PartyMember memberEntry = partyMemberRepository.findByPartyAndUser(party, memberA).orElseThrow();

        setAuth(memberA);
        UpdatePaymentStatusRequest paidRequest = paymentRequest(PaymentStatus.PAID);

        // when
        PartyDetailResponse paidResponse = partyService.updatePaymentStatus(party.getId(), memberEntry.getId(), paidRequest);

        // then
        assertThat(paidResponse.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);

        // when
        setAuth(host);
        PartyDetailResponse confirmedResponse = partyService.updatePaymentStatus(party.getId(), memberEntry.getId(), paymentRequest(PaymentStatus.CONFIRMED));
        PartyDetailResponse refundedResponse = partyService.updatePaymentStatus(party.getId(), memberEntry.getId(), paymentRequest(PaymentStatus.REFUNDED));

        // then
        assertThat(confirmedResponse.getSettlementMembers())
                .filteredOn(member -> member.getMemberId().equals(memberEntry.getId()))
                .singleElement()
                .satisfies(member -> assertThat(member.getPaymentStatus()).isEqualTo(PaymentStatus.CONFIRMED));
        assertThat(refundedResponse.getSettlementMembers())
                .filteredOn(member -> member.getMemberId().equals(memberEntry.getId()))
                .singleElement()
                .satisfies(member -> assertThat(member.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED));
    }

    @Test
    void 호스트가_픽업일정을_확정하고_참여자가_확인한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("픽업 테스트 지점"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User memberA = userRepository.saveAndFlush(createUser("member-a"));
        User memberB = userRepository.saveAndFlush(createUser("member-b"));
        Party party = createParty(store, host, memberA, memberB);
        setAuth(host);

        ConfirmPickupScheduleRequest pickupRequest = pickupRequest("양재역 8번 출구", LocalDateTime.now().plusHours(6));

        // when
        PartyDetailResponse hostResponse = partyService.confirmPickupSchedule(party.getId(), pickupRequest);

        // then
        assertThat(hostResponse.getPickupPlace()).isEqualTo("양재역 8번 출구");
        assertThat(hostResponse.getPickupTime()).isEqualTo(pickupRequest.getPickupTime());

        // when
        setAuth(memberA);
        PartyDetailResponse memberResponse = partyService.acknowledgePickup(party.getId());

        // then
        assertThat(memberResponse.isPickupAcknowledged()).isTrue();
    }

    @Test
    void 호스트가_거래완료와_노쇼를_기록하면_후기가능여부가_달라진다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("거래 상태 테스트 지점"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User memberA = userRepository.saveAndFlush(createUser("member-a"));
        User memberB = userRepository.saveAndFlush(createUser("member-b"));
        Party party = createParty(store, host, memberA, memberB);
        confirmSettlementAsHost(party, host, 28005);
        confirmPickupAsHost(party, host, "하남 스타필드 정문", LocalDateTime.now().plusHours(5));

        PartyMember memberEntryA = partyMemberRepository.findByPartyAndUser(party, memberA).orElseThrow();
        PartyMember memberEntryB = partyMemberRepository.findByPartyAndUser(party, memberB).orElseThrow();

        markPaidAsMember(party, memberA, memberEntryA.getId());
        markPaidAsMember(party, memberB, memberEntryB.getId());

        setAuth(host);
        partyService.updatePaymentStatus(party.getId(), memberEntryA.getId(), paymentRequest(PaymentStatus.CONFIRMED));
        partyService.updatePaymentStatus(party.getId(), memberEntryB.getId(), paymentRequest(PaymentStatus.CONFIRMED));

        // when
        PartyDetailResponse completedResponse = partyService.updateTradeStatus(party.getId(), memberEntryA.getId(), tradeRequest(TradeStatus.COMPLETED));
        PartyDetailResponse noShowResponse = partyService.updateTradeStatus(party.getId(), memberEntryB.getId(), tradeRequest(TradeStatus.NO_SHOW));

        // then
        assertThat(completedResponse.getSettlementMembers())
                .filteredOn(member -> member.getMemberId().equals(memberEntryA.getId()))
                .singleElement()
                .satisfies(member -> {
                    assertThat(member.getTradeStatus()).isEqualTo(TradeStatus.COMPLETED);
                    assertThat(member.isReviewEligible()).isTrue();
                });
        assertThat(noShowResponse.getSettlementMembers())
                .filteredOn(member -> member.getMemberId().equals(memberEntryB.getId()))
                .singleElement()
                .satisfies(member -> {
                    assertThat(member.getTradeStatus()).isEqualTo(TradeStatus.NO_SHOW);
                    assertThat(member.isReviewEligible()).isFalse();
                });
    }

    private Party createParty(Store store, User host, User memberA, User memberB) {
        Party party = new Party(
                "에픽 6 테스트 파티",
                "대용량 요거트",
                30003,
                store,
                4,
                "https://open.kakao.com/o/test",
                LocalDateTime.now().plusHours(8),
                "개",
                1,
                StorageType.REFRIGERATED,
                PackagingType.ORIGINAL_PACKAGE,
                true,
                false,
                "현장 정산 후 전달합니다.",
                null,
                null,
                null,
                null
        );
        party.acceptMember(PartyMember.joinAsHost(party, host, 1));
        party.acceptMember(PartyMember.joinAsMember(party, memberA, 1));
        party.acceptMember(PartyMember.joinAsMember(party, memberB, 2));
        return partyRepository.saveAndFlush(party);
    }

    private void confirmSettlementAsHost(Party party, User host, int actualTotalPrice) {
        setAuth(host);
        partyService.confirmSettlement(party.getId(), settlementRequest(actualTotalPrice, "테스트 정산"));
    }

    private void confirmPickupAsHost(Party party, User host, String pickupPlace, LocalDateTime pickupTime) {
        setAuth(host);
        partyService.confirmPickupSchedule(party.getId(), pickupRequest(pickupPlace, pickupTime));
    }

    private void markPaidAsMember(Party party, User user, Long memberId) {
        setAuth(user);
        partyService.updatePaymentStatus(party.getId(), memberId, paymentRequest(PaymentStatus.PAID));
    }

    private ConfirmSettlementRequest settlementRequest(int actualTotalPrice, String receiptNote) {
        ConfirmSettlementRequest request = new ConfirmSettlementRequest();
        ReflectionTestUtils.setField(request, "actualTotalPrice", actualTotalPrice);
        ReflectionTestUtils.setField(request, "receiptNote", receiptNote);
        return request;
    }

    private ConfirmPickupScheduleRequest pickupRequest(String pickupPlace, LocalDateTime pickupTime) {
        ConfirmPickupScheduleRequest request = new ConfirmPickupScheduleRequest();
        ReflectionTestUtils.setField(request, "pickupPlace", pickupPlace);
        ReflectionTestUtils.setField(request, "pickupTime", pickupTime);
        return request;
    }

    private UpdatePaymentStatusRequest paymentRequest(PaymentStatus paymentStatus) {
        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest();
        ReflectionTestUtils.setField(request, "paymentStatus", paymentStatus);
        return request;
    }

    private UpdateTradeStatusRequest tradeRequest(TradeStatus tradeStatus) {
        UpdateTradeStatusRequest request = new UpdateTradeStatusRequest();
        ReflectionTestUtils.setField(request, "tradeStatus", tradeStatus);
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
