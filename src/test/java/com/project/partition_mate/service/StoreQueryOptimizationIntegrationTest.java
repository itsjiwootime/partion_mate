package com.project.partition_mate.service;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.PartyResponse;
import com.project.partition_mate.dto.StoreResponse;
import com.project.partition_mate.repository.PartyRepository;
import com.project.partition_mate.repository.StoreRepository;
import com.project.partition_mate.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class StoreQueryOptimizationIntegrationTest {

    @Autowired
    private StoreService storeService;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoreQueryCacheSupport storeQueryCacheSupport;

    @AfterEach
    void tearDown() {
        storeQueryCacheSupport.clearAll();
        partyRepository.deleteAll();
        storeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 주변_지점_조회시_DB_거리순_정렬과_모집중_파티수를_반영한다() {
        // given
        Store closestStore = storeRepository.saveAndFlush(createStore("가장 가까운 지점", 37.5000, 127.0000));
        Store middleStore = storeRepository.saveAndFlush(createStore("중간 지점", 37.5400, 127.0300));
        Store farStore = storeRepository.saveAndFlush(createStore("가장 먼 지점", 37.5800, 127.0700));

        partyRepository.saveAndFlush(new Party("가까운 파티 1", "상품 A", 10000, closestStore, 5, "https://open.kakao.com/o/test"));
        partyRepository.saveAndFlush(new Party("가까운 파티 2", "상품 B", 12000, closestStore, 5, "https://open.kakao.com/o/test"));
        partyRepository.saveAndFlush(new Party("중간 파티", "상품 C", 14000, middleStore, 5, "https://open.kakao.com/o/test"));

        // when
        List<StoreResponse> responses = storeService.getNearbyStores(37.5005, 127.0005);

        // then
        assertThat(responses)
                .extracting(StoreResponse::getId)
                .containsExactly(closestStore.getId(), middleStore.getId(), farStore.getId());
        assertThat(responses)
                .extracting(StoreResponse::getPartyCount)
                .containsExactly(2L, 1L, 0L);
        assertThat(responses)
                .extracting(StoreResponse::getDistance)
                .allMatch(distance -> distance != null && distance >= 0.0);
    }

    @Test
    void 지점별_파티_조회시_현재수량을_포함한_projection을_반환한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("테스트 지점", 37.5000, 127.0000));
        User host = userRepository.saveAndFlush(createUser("host"));
        User member = userRepository.saveAndFlush(createUser("member"));

        Party firstParty = new Party("첫 번째 파티", "상품 A", 10000, store, 5, "https://open.kakao.com/o/test");
        firstParty.acceptMember(PartyMember.joinAsHost(firstParty, host, 1));
        firstParty.acceptMember(PartyMember.joinAsMember(firstParty, member, 2));

        Party secondParty = new Party("두 번째 파티", "상품 B", 20000, store, 6, "https://open.kakao.com/o/test");
        secondParty.acceptMember(PartyMember.joinAsHost(secondParty, host, 1));

        partyRepository.saveAndFlush(firstParty);
        partyRepository.saveAndFlush(secondParty);

        // when
        List<PartyResponse> responses = storeService.findPartiesByStoreId(store.getId());

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses)
                .extracting(PartyResponse::getStoreName)
                .containsOnly(store.getName());
        assertThat(responses)
                .filteredOn(response -> response.getTitle().equals("첫 번째 파티"))
                .singleElement()
                .satisfies(response -> assertThat(response.getCurrentQuantity()).isEqualTo(3));
        assertThat(responses)
                .filteredOn(response -> response.getTitle().equals("두 번째 파티"))
                .singleElement()
                .satisfies(response -> assertThat(response.getCurrentQuantity()).isEqualTo(1));
    }

    private Store createStore(String name, double latitude, double longitude) {
        return new Store(
                name,
                "서울시 테스트로",
                LocalTime.of(10, 0),
                LocalTime.of(22, 0),
                latitude,
                longitude,
                "02-0000-0000"
        );
    }

    private User createUser(String username) {
        return new User(username, username + "@test.com", "pw", "서울", 37.5, 127.0);
    }
}
