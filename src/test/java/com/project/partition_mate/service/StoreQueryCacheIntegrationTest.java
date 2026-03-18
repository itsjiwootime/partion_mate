package com.project.partition_mate.service;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.CreatePartyRequest;
import com.project.partition_mate.dto.JoinPartyRequest;
import com.project.partition_mate.repository.PartyRepository;
import com.project.partition_mate.repository.StoreRepository;
import com.project.partition_mate.repository.UserRepository;
import com.project.partition_mate.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class StoreQueryCacheIntegrationTest {

    @Autowired
    private StoreService storeService;

    @Autowired
    private PartyService partyService;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoreQueryCacheSupport storeQueryCacheSupport;

    @MockitoSpyBean
    private StoreRepository spiedStoreRepository;

    @MockitoSpyBean
    private PartyRepository spiedPartyRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        storeQueryCacheSupport.clearAll();
        Mockito.reset(spiedStoreRepository, spiedPartyRepository);
        partyRepository.deleteAll();
        storeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 같은_좌표로_주변지점을_반복조회하면_캐시를_재사용한다() {
        // given
        storeRepository.saveAndFlush(createStore("지점 A", 37.5000, 127.0000));
        clearInvocations(spiedStoreRepository);

        // when
        storeService.getNearbyStores(37.5000, 127.0000);
        storeService.getNearbyStores(37.5000, 127.0000);

        // then
        verify(spiedStoreRepository, times(1))
                .findNearbyStoresWithinBounds(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyString());
    }

    @Test
    void 파티를_생성하면_주변지점과_지점별파티_캐시를_무효화한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("지점 A", 37.5000, 127.0000));
        User host = userRepository.saveAndFlush(createUser("host"));
        setAuth(host);

        storeService.getNearbyStores(37.5000, 127.0000);
        storeService.findPartiesByStoreId(store.getId());
        clearInvocations(spiedStoreRepository, spiedPartyRepository);

        CreatePartyRequest request = new CreatePartyRequest();
        ReflectionTestUtils.setField(request, "title", "신규 파티");
        ReflectionTestUtils.setField(request, "storeId", store.getId());
        ReflectionTestUtils.setField(request, "productName", "새 상품");
        ReflectionTestUtils.setField(request, "totalPrice", 12000);
        ReflectionTestUtils.setField(request, "totalQuantity", 5);
        ReflectionTestUtils.setField(request, "hostRequestedQuantity", 1);
        ReflectionTestUtils.setField(request, "unitLabel", "개");
        ReflectionTestUtils.setField(request, "minimumShareUnit", 1);
        ReflectionTestUtils.setField(request, "storageType", com.project.partition_mate.domain.StorageType.ROOM_TEMPERATURE);
        ReflectionTestUtils.setField(request, "packagingType", com.project.partition_mate.domain.PackagingType.ORIGINAL_PACKAGE);
        ReflectionTestUtils.setField(request, "hostProvidesPackaging", true);
        ReflectionTestUtils.setField(request, "onSiteSplit", false);
        ReflectionTestUtils.setField(request, "guideNote", "기본 안내");

        // when
        partyService.createParty(request);
        storeService.getNearbyStores(37.5000, 127.0000);
        storeService.findPartiesByStoreId(store.getId());

        // then
        verify(spiedStoreRepository, times(1))
                .findNearbyStoresWithinBounds(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyString());
        verify(spiedPartyRepository, times(1)).findResponsesByStoreId(store.getId());
    }

    @Test
    void 파티에_즉시_참여하면_주변지점과_지점별파티_캐시를_무효화한다() {
        // given
        Store store = storeRepository.saveAndFlush(createStore("지점 A", 37.5000, 127.0000));
        User host = userRepository.saveAndFlush(createUser("host"));
        User member = userRepository.saveAndFlush(createUser("member"));

        Party party = new Party("테스트 파티", "상품 A", 10000, store, 3, "https://open.kakao.com/o/test");
        party.acceptMember(PartyMember.joinAsHost(party, host, 1));
        partyRepository.saveAndFlush(party);

        storeService.getNearbyStores(37.5000, 127.0000);
        storeService.findPartiesByStoreId(store.getId());
        clearInvocations(spiedStoreRepository, spiedPartyRepository);

        JoinPartyRequest request = new JoinPartyRequest();
        ReflectionTestUtils.setField(request, "memberRequestQuantity", 1);
        setAuth(member);

        // when
        partyService.joinParty(party.getId(), request);
        storeService.getNearbyStores(37.5000, 127.0000);
        storeService.findPartiesByStoreId(store.getId());

        // then
        verify(spiedStoreRepository, times(1))
                .findNearbyStoresWithinBounds(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyString());
        verify(spiedPartyRepository, times(1)).findResponsesByStoreId(store.getId());
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

    private void setAuth(User user) {
        CustomUserDetails principal = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
