package com.project.partition_mate.service;

import com.project.partition_mate.domain.Store;
import com.project.partition_mate.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "app.seed.stores.enabled=false")
@ActiveProfiles("test")
class StoreSeedServiceIntegrationTest {

    @Autowired
    private StoreSeedService storeSeedService;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ResourceLoader resourceLoader;

    @BeforeEach
    void setUp() {
        storeRepository.deleteAll();
    }

    @Test
    void 지점_시드_파일을_적재한다() {
        // given
        var resource = resourceLoader.getResource("classpath:seeds/test-stores.json");

        // when
        StoreSeedService.SeedResult result = storeSeedService.synchronize(resource);

        // then
        assertThat(result.createdCount()).isEqualTo(2);
        assertThat(result.updatedCount()).isZero();
        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(storeRepository.findAll())
                .extracting(Store::getName)
                .containsExactlyInAnyOrder("코스트코 테스트점", "트레이더스 테스트점");
    }

    @Test
    void 같은_지점명이_있으면_기존_지점을_업데이트한다() {
        // given
        Store existingStore = storeRepository.saveAndFlush(new Store(
                "코스트코 테스트점",
                "이전 주소",
                LocalTime.of(9, 0),
                LocalTime.of(21, 0),
                37.1000,
                127.1000,
                "02-0000-0000"
        ));
        var resource = resourceLoader.getResource("classpath:seeds/test-stores.json");

        // when
        StoreSeedService.SeedResult result = storeSeedService.synchronize(resource);

        // then
        Store updatedStore = storeRepository.findByName("코스트코 테스트점").orElseThrow();
        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(result.updatedCount()).isEqualTo(1);
        assertThat(storeRepository.count()).isEqualTo(2);
        assertThat(updatedStore.getId()).isEqualTo(existingStore.getId());
        assertThat(updatedStore.getAddress()).isEqualTo("서울특별시 서초구 테스트로 1");
        assertThat(updatedStore.getOpenTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(updatedStore.getCloseTime()).isEqualTo(LocalTime.of(22, 0));
        assertThat(updatedStore.getPhone()).isEqualTo("1899-9900");
        assertThat(updatedStore.getLatitude()).isEqualTo(37.4620);
        assertThat(updatedStore.getLongitude()).isEqualTo(127.0366);
    }
}
