package com.project.partition_mate.service;

import com.project.partition_mate.domain.PackagingType;
import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyCloseReason;
import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.domain.PartyStatus;
import com.project.partition_mate.domain.StorageType;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.ConfirmPickupScheduleRequest;
import com.project.partition_mate.dto.PartyDetailResponse;
import com.project.partition_mate.dto.UpdatePartyRequest;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
@ActiveProfiles("test")
class PartyUpdateIntegrationTest {

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
    void 호스트가_파티를_수정하면_핵심정보를_갱신한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("양재 코스트코"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User member = userRepository.saveAndFlush(createUser("member"));
        Party party = createFullParty(store, host, member);
        setAuth(host);
        UpdatePartyRequest request = updateRequest(
                "비타민 대용량 소분",
                "비타민 C 1000정",
                32000,
                4,
                "https://open.kakao.com/o/updated-room",
                LocalDateTime.now().plusHours(10),
                "통",
                1,
                StorageType.ROOM_TEMPERATURE,
                PackagingType.CONTAINER,
                false,
                true,
                "현장 분배 후 전달합니다."
        );

        // when
        PartyDetailResponse response = partyService.updateParty(party.getId(), request);

        // then
        assertThat(response.getTitle()).isEqualTo("비타민 대용량 소분");
        assertThat(response.getProductName()).isEqualTo("비타민 C 1000정");
        assertThat(response.getExpectedTotalPrice()).isEqualTo(32000);
        assertThat(response.getTotalQuantity()).isEqualTo(4);
        assertThat(response.getOpenChatUrl()).isEqualTo("https://open.kakao.com/o/updated-room");
        assertThat(response.getUnitLabel()).isEqualTo("통");
        assertThat(response.getPackagingType()).isEqualTo(PackagingType.CONTAINER);
        assertThat(response.isOnSiteSplit()).isTrue();
        assertThat(response.getGuideNote()).isEqualTo("현장 분배 후 전달합니다.");
        assertThat(response.getStatus()).isEqualTo(PartyStatus.RECRUITING);
    }

    @Test
    void 참여자가_파티를_수정하면_예외가_발생한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("하남 트레이더스"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User member = userRepository.saveAndFlush(createUser("member"));
        Party party = createRecruitingParty(store, host, member);
        setAuth(member);
        UpdatePartyRequest request = defaultUpdateRequest();

        // when
        Throwable thrown = catchThrowable(() -> partyService.updateParty(party.getId(), request));

        // then
        assertThat(thrown)
                .isInstanceOf(BusinessException.class)
                .hasMessage("파티 수정은 호스트만 처리할 수 있습니다.");
    }

    @Test
    void 종료된_파티를_수정하면_예외가_발생한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("송도 코스트코"));
        User host = userRepository.saveAndFlush(createUser("host"));
        Party party = createRecruitingParty(store, host, null);
        party.close(LocalDateTime.now().minusMinutes(1), PartyCloseReason.DEADLINE_EXPIRED);
        partyRepository.saveAndFlush(party);
        setAuth(host);
        UpdatePartyRequest request = defaultUpdateRequest();

        // when
        Throwable thrown = catchThrowable(() -> partyService.updateParty(party.getId(), request));

        // then
        assertThat(thrown)
                .isInstanceOf(BusinessException.class)
                .hasMessage("이미 종료된 파티입니다.");
    }

    @Test
    void 픽업이_확정된_뒤에도_안내문과_오픈채팅링크는_수정한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("세종 코스트코"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User member = userRepository.saveAndFlush(createUser("member"));
        Party party = createRecruitingParty(store, host, member);
        setAuth(host);
        partyService.confirmPickupSchedule(party.getId(), pickupRequest("세종터미널 앞", LocalDateTime.now().plusHours(6)));
        UpdatePartyRequest request = updateRequest(
                party.getTitle(),
                party.getProductName(),
                party.getExpectedTotalPrice(),
                party.getTotalQuantity(),
                "https://open.kakao.com/o/after-pickup",
                party.getDeadline(),
                party.getUnitLabel(),
                party.getMinimumShareUnit(),
                party.getStorageType(),
                party.getPackagingType(),
                party.isHostProvidesPackaging(),
                party.isOnSiteSplit(),
                "픽업 10분 전 도착 부탁드립니다."
        );

        // when
        PartyDetailResponse response = partyService.updateParty(party.getId(), request);

        // then
        assertThat(response.getOpenChatUrl()).isEqualTo("https://open.kakao.com/o/after-pickup");
        assertThat(response.getGuideNote()).isEqualTo("픽업 10분 전 도착 부탁드립니다.");
        assertThat(response.getPickupPlace()).isEqualTo("세종터미널 앞");
    }

    @Test
    void 픽업이_확정된_뒤_거래조건을_수정하면_예외가_발생한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("부산 트레이더스"));
        User host = userRepository.saveAndFlush(createUser("host"));
        User member = userRepository.saveAndFlush(createUser("member"));
        Party party = createRecruitingParty(store, host, member);
        setAuth(host);
        partyService.confirmPickupSchedule(party.getId(), pickupRequest("부산역 1번 출구", LocalDateTime.now().plusHours(5)));
        UpdatePartyRequest request = updateRequest(
                "수정된 제목",
                party.getProductName(),
                party.getExpectedTotalPrice(),
                party.getTotalQuantity(),
                party.getOpenChatUrl(),
                party.getDeadline().plusHours(2),
                party.getUnitLabel(),
                party.getMinimumShareUnit(),
                party.getStorageType(),
                party.getPackagingType(),
                party.isHostProvidesPackaging(),
                party.isOnSiteSplit(),
                party.getGuideNote()
        );

        // when
        Throwable thrown = catchThrowable(() -> partyService.updateParty(party.getId(), request));

        // then
        assertThat(thrown)
                .isInstanceOf(BusinessException.class)
                .hasMessage("정산 또는 픽업 일정이 진행된 뒤에는 안내 문구와 오픈채팅 링크만 수정할 수 있습니다.");
    }

    private Party createFullParty(Store store, User host, User member) {
        Party party = new Party(
                "기존 파티",
                "비타민",
                28000,
                store,
                2,
                "https://open.kakao.com/o/base-room",
                LocalDateTime.now().plusHours(8),
                "개",
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

    private Party createRecruitingParty(Store store, User host, User member) {
        Party party = new Party(
                "기존 파티",
                "대용량 요거트",
                30000,
                store,
                4,
                "https://open.kakao.com/o/base-room",
                LocalDateTime.now().plusHours(8),
                "개",
                1,
                StorageType.REFRIGERATED,
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
        if (member != null) {
            party.acceptMember(PartyMember.joinAsMember(party, member, 1));
        }
        return partyRepository.saveAndFlush(party);
    }

    private UpdatePartyRequest defaultUpdateRequest() {
        return updateRequest(
                "수정된 제목",
                "수정된 제품명",
                31000,
                5,
                "https://open.kakao.com/o/updated-room",
                LocalDateTime.now().plusHours(12),
                "팩",
                1,
                StorageType.FROZEN,
                PackagingType.ZIP_BAG,
                true,
                true,
                "수정된 안내"
        );
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
                                             Boolean hostProvidesPackaging,
                                             Boolean onSiteSplit,
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

    private ConfirmPickupScheduleRequest pickupRequest(String pickupPlace, LocalDateTime pickupTime) {
        ConfirmPickupScheduleRequest request = new ConfirmPickupScheduleRequest();
        ReflectionTestUtils.setField(request, "pickupPlace", pickupPlace);
        ReflectionTestUtils.setField(request, "pickupTime", pickupTime);
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
