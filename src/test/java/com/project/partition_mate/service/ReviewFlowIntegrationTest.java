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
import com.project.partition_mate.dto.CreateReviewRequest;
import com.project.partition_mate.dto.PartyDetailResponse;
import com.project.partition_mate.dto.UpdatePaymentStatusRequest;
import com.project.partition_mate.dto.UpdateTradeStatusRequest;
import com.project.partition_mate.exception.BusinessException;
import com.project.partition_mate.repository.PartyMemberRepository;
import com.project.partition_mate.repository.PartyRepository;
import com.project.partition_mate.repository.ReviewRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

@SpringBootTest
@ActiveProfiles("test")
class ReviewFlowIntegrationTest {

    @Autowired
    private PartyService partyService;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private PartyMemberRepository partyMemberRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        reviewRepository.deleteAll();
        partyMemberRepository.deleteAll();
        partyRepository.deleteAll();
        storeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 거래완료된_참여자는_호스트에게_후기를_1회만_작성할_수_있다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("후기 테스트 지점"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User member = userRepository.saveAndFlush(createUser("member"));
        Party party = createParty(store, host, member);
        completeTradeForMember(party, host, member);
        setAuth(member);
        CreateReviewRequest request = reviewRequest(host.getId(), 5, "응답이 빠르고 정산이 정확했습니다.");

        // when
        PartyDetailResponse response = partyService.submitReview(party.getId(), request);

        // then
        assertThat(reviewRepository.count()).isEqualTo(1);
        assertThat(response.isCanReviewHost()).isTrue();
        assertThat(response.isHasReviewedHost()).isTrue();
        assertThat(response.getHostTrust().getReviewCount()).isEqualTo(1);
        assertThat(response.getHostReviews())
                .extracting(review -> review.getReviewerName(), review -> review.getRating())
                .containsExactly(tuple("member", 5));

        // when & then
        assertThatThrownBy(() -> partyService.submitReview(party.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("같은 파티에서 이미 작성한 후기입니다.");
    }

    @Test
    void 거래완료되지_않으면_후기를_작성할_수_없다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("후기 제한 지점"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User member = userRepository.saveAndFlush(createUser("member"));
        Party party = createParty(store, host, member);
        setAuth(member);
        CreateReviewRequest request = reviewRequest(host.getId(), 4, "아직 거래가 끝나지 않았습니다.");

        // when & then
        assertThatThrownBy(() -> partyService.submitReview(party.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("거래 완료된 상대에게만 후기를 작성할 수 있습니다.");
    }

    @Test
    void 호스트는_거래완료된_참여자에게만_후기를_작성할_수_있다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("호스트 후기 지점"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User memberA = userRepository.saveAndFlush(createUser("member-a"));
        User memberB = userRepository.saveAndFlush(createUser("member-b"));
        Party party = createParty(store, host, memberA, memberB);
        completeTradeForMember(party, host, memberA);
        setAuth(host);
        PartyMember memberBEntry = partyMemberRepository.findByPartyAndUser(party, memberB).orElseThrow();
        partyService.updateTradeStatus(party.getId(), memberBEntry.getId(), tradeRequest(TradeStatus.NO_SHOW));
        CreateReviewRequest successRequest = reviewRequest(memberA.getId(), 5, "약속 시간을 잘 지켜주셨어요.");
        CreateReviewRequest failRequest = reviewRequest(memberB.getId(), 2, "노쇼였습니다.");

        // when
        PartyDetailResponse response = partyService.submitReview(party.getId(), successRequest);

        // then
        assertThat(response.getSettlementMembers())
                .filteredOn(memberResponse -> memberResponse.getUserId().equals(memberA.getId()))
                .singleElement()
                .satisfies(memberResponse -> assertThat(memberResponse.isReviewWritten()).isTrue());

        // when & then
        assertThatThrownBy(() -> partyService.submitReview(party.getId(), failRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessage("거래 완료된 상대에게만 후기를 작성할 수 있습니다.");
    }

    private Party createParty(Store store, User host, User... members) {
        Party party = new Party(
                "에픽 7 테스트 파티",
                "대용량 그릭요거트",
                24000,
                store,
                members.length + 1,
                "https://open.kakao.com/o/test",
                LocalDateTime.now().plusHours(8),
                "개",
                1,
                StorageType.REFRIGERATED,
                PackagingType.ORIGINAL_PACKAGE,
                true,
                false,
                "거래 완료 후 후기 테스트",
                null,
                null,
                null,
                null
        );
        party.acceptMember(PartyMember.joinAsHost(party, host, 1));
        for (User member : members) {
            party.acceptMember(PartyMember.joinAsMember(party, member, 1));
        }
        return partyRepository.saveAndFlush(party);
    }

    private void completeTradeForMember(Party party, User host, User member) {
        PartyMember memberEntry = partyMemberRepository.findByPartyAndUser(party, member).orElseThrow();

        setAuth(host);
        partyService.confirmSettlement(party.getId(), settlementRequest(24000, "테스트 정산"));
        partyService.confirmPickupSchedule(party.getId(), pickupRequest("양재 시민의숲역", LocalDateTime.now().plusHours(4)));

        setAuth(member);
        partyService.updatePaymentStatus(party.getId(), memberEntry.getId(), paymentRequest(PaymentStatus.PAID));

        setAuth(host);
        partyService.updatePaymentStatus(party.getId(), memberEntry.getId(), paymentRequest(PaymentStatus.CONFIRMED));
        partyService.updateTradeStatus(party.getId(), memberEntry.getId(), tradeRequest(TradeStatus.COMPLETED));
    }

    private CreateReviewRequest reviewRequest(Long targetUserId, int rating, String comment) {
        CreateReviewRequest request = new CreateReviewRequest();
        ReflectionTestUtils.setField(request, "targetUserId", targetUserId);
        ReflectionTestUtils.setField(request, "rating", rating);
        ReflectionTestUtils.setField(request, "comment", comment);
        return request;
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
