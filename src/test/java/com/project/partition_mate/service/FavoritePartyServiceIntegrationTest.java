package com.project.partition_mate.service;

import com.project.partition_mate.domain.PackagingType;
import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.StorageType;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.PartyDetailResponse;
import com.project.partition_mate.dto.PartyResponse;
import com.project.partition_mate.repository.FavoritePartyRepository;
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

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class FavoritePartyServiceIntegrationTest {

    @Autowired
    private FavoritePartyService favoritePartyService;

    @Autowired
    private PartyService partyService;

    @Autowired
    private StoreService storeService;

    @Autowired
    private FavoritePartyRepository favoritePartyRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        favoritePartyRepository.deleteAll();
        partyRepository.deleteAll();
        storeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 관심_파티를_저장하면_내_관심_파티_목록에_저장한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("양재 코스트코"));
        User viewer = userRepository.saveAndFlush(createUser("viewer"));
        Party party = partyRepository.saveAndFlush(createParty(store, "연어 소분"));

        // when
        favoritePartyService.save(viewer, party.getId());
        favoritePartyService.save(viewer, party.getId());
        List<PartyResponse> favorites = favoritePartyService.getFavoriteParties(viewer);

        // then
        assertThat(favoritePartyRepository.count()).isEqualTo(1);
        assertThat(favorites)
                .hasSize(1)
                .allSatisfy(response -> {
                    assertThat(response.getId()).isEqualTo(party.getId());
                    assertThat(response.isFavorite()).isTrue();
                });
    }

    @Test
    void 관심_파티를_저장한_사용자가_상세를_조회하면_관심_상태를_보여준다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("하남 트레이더스"));
        User viewer = userRepository.saveAndFlush(createUser("viewer"));
        Party party = partyRepository.saveAndFlush(createParty(store, "냉동만두 소분"));
        favoritePartyService.save(viewer, party.getId());
        setAuth(viewer);

        // when
        PartyDetailResponse response = partyService.detailsParty(party.getId());

        // then
        assertThat(response.getId()).isEqualTo(party.getId());
        assertThat(response.isFavorite()).isTrue();
    }

    @Test
    void 지점_파티_목록을_조회하면_관심_파티만_favorite로_표시한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("광명 코스트코"));
        User viewer = userRepository.saveAndFlush(createUser("viewer"));
        Party savedFavorite = partyRepository.saveAndFlush(createParty(store, "휴지 공동구매"));
        Party normalParty = partyRepository.saveAndFlush(createParty(store, "주방세제 공동구매"));
        favoritePartyService.save(viewer, savedFavorite.getId());

        // when
        List<PartyResponse> responses = storeService.findPartiesByStoreId(store.getId(), viewer);

        // then
        assertThat(responses)
                .hasSize(2)
                .filteredOn(response -> response.getId().equals(savedFavorite.getId()))
                .singleElement()
                .satisfies(response -> assertThat(response.isFavorite()).isTrue());
        assertThat(responses)
                .filteredOn(response -> response.getId().equals(normalParty.getId()))
                .singleElement()
                .satisfies(response -> assertThat(response.isFavorite()).isFalse());
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

    private Party createParty(Store store, String title) {
        return new Party(
                title,
                title + " 상품",
                24000,
                store,
                4,
                "https://open.kakao.com/o/favorite",
                LocalDateTime.now().plusHours(6),
                "개",
                1,
                StorageType.ROOM_TEMPERATURE,
                PackagingType.ORIGINAL_PACKAGE,
                false,
                false,
                "안내 메모",
                null,
                null,
                null,
                null
        );
    }

    private void setAuth(User user) {
        CustomUserDetails principal = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
