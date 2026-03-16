package com.project.partition_mate.service;

import com.project.partition_mate.domain.PackagingType;
import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.StorageType;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.CreatePartyRequest;
import com.project.partition_mate.dto.PartyDetailResponse;
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
class PartyMetadataIntegrationTest {

    @Autowired
    private PartyService partyService;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        partyRepository.deleteAll();
        storeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 파티를_생성하면_소분_메타데이터와_거래안내를_저장한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("양재 코스트코"));
        User host = userRepository.saveAndFlush(createUser("host"));
        setAuth(host);

        CreatePartyRequest request = new CreatePartyRequest();
        ReflectionTestUtils.setField(request, "title", "닭가슴살 소분");
        ReflectionTestUtils.setField(request, "storeId", store.getId());
        ReflectionTestUtils.setField(request, "productName", "닭가슴살 5kg");
        ReflectionTestUtils.setField(request, "totalPrice", 42000);
        ReflectionTestUtils.setField(request, "totalQuantity", 10);
        ReflectionTestUtils.setField(request, "hostRequestedQuantity", 2);
        ReflectionTestUtils.setField(request, "openChatUrl", "https://open.kakao.com/o/test");
        ReflectionTestUtils.setField(request, "deadline", LocalDateTime.now().plusHours(8));
        ReflectionTestUtils.setField(request, "unitLabel", "팩");
        ReflectionTestUtils.setField(request, "minimumShareUnit", 1);
        ReflectionTestUtils.setField(request, "storageType", StorageType.FROZEN);
        ReflectionTestUtils.setField(request, "packagingType", PackagingType.ZIP_BAG);
        ReflectionTestUtils.setField(request, "hostProvidesPackaging", true);
        ReflectionTestUtils.setField(request, "onSiteSplit", true);
        ReflectionTestUtils.setField(request, "guideNote", "보냉백 지참 부탁드립니다.");

        // when
        Party savedParty = partyService.createParty(request);
        PartyDetailResponse detailResponse = partyService.detailsParty(savedParty.getId());

        // then
        assertThat(savedParty.getUnitLabel()).isEqualTo("팩");
        assertThat(savedParty.getMinimumShareUnit()).isEqualTo(1);
        assertThat(savedParty.getStorageType()).isEqualTo(StorageType.FROZEN);
        assertThat(savedParty.getPackagingType()).isEqualTo(PackagingType.ZIP_BAG);
        assertThat(savedParty.isHostProvidesPackaging()).isTrue();
        assertThat(savedParty.isOnSiteSplit()).isTrue();
        assertThat(savedParty.getGuideNote()).isEqualTo("보냉백 지참 부탁드립니다.");
        assertThat(detailResponse.getUnitLabel()).isEqualTo("팩");
        assertThat(detailResponse.getStorageTypeLabel()).isEqualTo("냉동");
        assertThat(detailResponse.getPackagingTypeLabel()).isEqualTo("지퍼백");
        assertThat(detailResponse.isHostProvidesPackaging()).isTrue();
        assertThat(detailResponse.isOnSiteSplit()).isTrue();
        assertThat(detailResponse.getGuideNote()).isEqualTo("보냉백 지참 부탁드립니다.");
    }

    @Test
    void 실구매가를_저장하면_상세응답에서_예상가와_실구매가를_함께_보여준다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("하남 트레이더스"));
        Party party = partyRepository.saveAndFlush(new Party(
                "비타민 소분",
                "비타민 C",
                30000,
                store,
                4,
                "https://open.kakao.com/o/test",
                LocalDateTime.now().plusHours(5),
                "개",
                1,
                StorageType.ROOM_TEMPERATURE,
                PackagingType.ORIGINAL_PACKAGE,
                false,
                false,
                "영수증 확인 후 정산합니다.",
                null,
                null
        ));
        party.updateActualPurchase(26500, "행사 할인가 적용");
        partyRepository.saveAndFlush(party);

        // when
        PartyDetailResponse detailResponse = partyService.detailsParty(party.getId());

        // then
        assertThat(detailResponse.getTotalPrice()).isEqualTo(26500);
        assertThat(detailResponse.getExpectedTotalPrice()).isEqualTo(30000);
        assertThat(detailResponse.getActualTotalPrice()).isEqualTo(26500);
        assertThat(detailResponse.getReceiptNote()).isEqualTo("행사 할인가 적용");
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
