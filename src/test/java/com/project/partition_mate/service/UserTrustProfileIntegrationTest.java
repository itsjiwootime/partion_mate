package com.project.partition_mate.service;

import com.project.partition_mate.domain.PackagingType;
import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.domain.PaymentStatus;
import com.project.partition_mate.domain.StorageType;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.domain.TradeStatus;
import com.project.partition_mate.domain.TrustLevel;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.ConfirmPickupScheduleRequest;
import com.project.partition_mate.dto.ConfirmSettlementRequest;
import com.project.partition_mate.dto.CreateReviewRequest;
import com.project.partition_mate.dto.PartyDetailResponse;
import com.project.partition_mate.dto.UpdatePaymentStatusRequest;
import com.project.partition_mate.dto.UpdateTradeStatusRequest;
import com.project.partition_mate.dto.UserResponse;
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
import static org.assertj.core.groups.Tuple.tuple;

@SpringBootTest
@ActiveProfiles("test")
class UserTrustProfileIntegrationTest {

    @Autowired
    private PartyService partyService;

    @Autowired
    private UserService userService;

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
    void 프로필과_파티상세에서_호스트_신뢰도와_최근후기를_노출한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("신뢰도 프로필 지점"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User member = userRepository.saveAndFlush(createUser("member"));
        Party party = createParty(store, host, member);
        completeTradeForMember(party, host, member);

        setAuth(member);
        partyService.submitReview(party.getId(), reviewRequest(host.getId(), 5, "정산과 전달이 모두 깔끔했습니다."));
        SecurityContextHolder.clearContext();

        // when
        UserResponse profile = userService.getProfile(host);
        PartyDetailResponse detail = partyService.detailsParty(party.getId());

        // then
        assertThat(profile.getTrustSummary().getAverageRating()).isEqualTo(5.0);
        assertThat(profile.getTrustSummary().getReviewCount()).isEqualTo(1);
        assertThat(profile.getTrustSummary().getCompletedTradeCount()).isEqualTo(1);
        assertThat(profile.getTrustSummary().getTrustLevel()).isEqualTo(TrustLevel.TOP);
        assertThat(profile.getRecentReviews())
                .extracting(review -> review.getReviewerName(), review -> review.getRating())
                .containsExactly(tuple("member", 5));

        assertThat(detail.getHostTrust().getReviewCount()).isEqualTo(1);
        assertThat(detail.getHostTrust().getTrustLevel()).isEqualTo(TrustLevel.TOP);
        assertThat(detail.getHostReviews())
                .extracting(review -> review.getReviewerName(), review -> review.getComment())
                .containsExactly(tuple("member", "정산과 전달이 모두 깔끔했습니다."));
    }

    @Test
    void 노쇼가_기록된_사용자는_주의_등급으로_계산된다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("노쇼 지점"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User noShowMember = userRepository.saveAndFlush(createUser("member"));
        Party party = createParty(store, host, noShowMember);
        prepareSettlementAndPickup(party, host);
        PartyMember memberEntry = partyMemberRepository.findByPartyAndUser(party, noShowMember).orElseThrow();

        setAuth(noShowMember);
        partyService.updatePaymentStatus(party.getId(), memberEntry.getId(), paymentRequest(PaymentStatus.PAID));

        setAuth(host);
        partyService.updatePaymentStatus(party.getId(), memberEntry.getId(), paymentRequest(PaymentStatus.CONFIRMED));
        partyService.updateTradeStatus(party.getId(), memberEntry.getId(), tradeRequest(TradeStatus.NO_SHOW));

        // when
        UserResponse profile = userService.getProfile(noShowMember);

        // then
        assertThat(profile.getTrustSummary().getNoShowCount()).isEqualTo(1);
        assertThat(profile.getTrustSummary().getCompletionRate()).isEqualTo(0);
        assertThat(profile.getTrustSummary().getTrustScore()).isEqualTo(0);
        assertThat(profile.getTrustSummary().getTrustLevel()).isEqualTo(TrustLevel.CAUTION);
    }

    private Party createParty(Store store, User host, User member) {
        Party party = new Party(
                "에픽 7 신뢰도 파티",
                "대용량 닭가슴살",
                28000,
                store,
                2,
                "https://open.kakao.com/o/test",
                LocalDateTime.now().plusHours(6),
                "팩",
                1,
                StorageType.FROZEN,
                PackagingType.ZIP_BAG,
                true,
                true,
                "신뢰도 테스트용 파티",
                null,
                null,
                null,
                null
        );
        party.acceptMember(PartyMember.joinAsHost(party, host, 1));
        party.acceptMember(PartyMember.joinAsMember(party, member, 1));
        return partyRepository.saveAndFlush(party);
    }

    private void completeTradeForMember(Party party, User host, User member) {
        prepareSettlementAndPickup(party, host);
        PartyMember memberEntry = partyMemberRepository.findByPartyAndUser(party, member).orElseThrow();

        setAuth(member);
        partyService.updatePaymentStatus(party.getId(), memberEntry.getId(), paymentRequest(PaymentStatus.PAID));

        setAuth(host);
        partyService.updatePaymentStatus(party.getId(), memberEntry.getId(), paymentRequest(PaymentStatus.CONFIRMED));
        partyService.updateTradeStatus(party.getId(), memberEntry.getId(), tradeRequest(TradeStatus.COMPLETED));
    }

    private void prepareSettlementAndPickup(Party party, User host) {
        setAuth(host);
        partyService.confirmSettlement(party.getId(), settlementRequest(28000, "정산 완료"));
        partyService.confirmPickupSchedule(party.getId(), pickupRequest("하남 스타필드 정문", LocalDateTime.now().plusHours(5)));
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
