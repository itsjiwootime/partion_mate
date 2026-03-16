package com.project.partition_mate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.repository.PartyRepository;
import com.project.partition_mate.repository.StoreRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StoreQueryBaselineBenchmarkTest {

    private static final int STORE_COUNT = 300;
    private static final int PARTIES_PER_STORE = 6;
    private static final int WARM_UP_RUNS = 5;
    private static final int MEASURED_RUNS = 20;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private PartyRepository partyRepository;

    @AfterEach
    void tearDown() {
        partyRepository.deleteAll();
        storeRepository.deleteAll();
    }

    @Test
    void 지점과_파티_조회_성능_기준값을_기록한다() throws Exception {
        // given
        SeedResult seedResult = seedStoresAndParties();

        // when
        BenchmarkResult nearbyResult = benchmarkNearbyStores(seedResult.referenceLatitude(), seedResult.referenceLongitude());
        BenchmarkResult storePartiesResult = benchmarkStoreParties(seedResult.targetStoreId());

        // then
        assertThat(nearbyResult.resultSize()).isEqualTo(STORE_COUNT);
        assertThat(storePartiesResult.resultSize()).isEqualTo(PARTIES_PER_STORE);
        assertThat(nearbyResult.averageMillis()).isGreaterThan(0.0);
        assertThat(storePartiesResult.averageMillis()).isGreaterThan(0.0);

        System.out.printf(
                "STORE_QUERY_BASELINE nearbyAvgMs=%.2f nearbyP95Ms=%.2f nearbyCount=%d storePartiesAvgMs=%.2f storePartiesP95Ms=%.2f storePartiesCount=%d%n",
                nearbyResult.averageMillis(),
                nearbyResult.p95Millis(),
                nearbyResult.resultSize(),
                storePartiesResult.averageMillis(),
                storePartiesResult.p95Millis(),
                storePartiesResult.resultSize()
        );
    }

    private SeedResult seedStoresAndParties() {
        List<Store> stores = new ArrayList<>();

        for (int i = 0; i < STORE_COUNT; i++) {
            stores.add(new Store(
                    "벤치마크 지점 " + i,
                    "서울시 벤치마크로 " + i,
                    LocalTime.of(10, 0),
                    LocalTime.of(22, 0),
                    37.40 + (i * 0.001),
                    127.00 + (i * 0.001),
                    "02-0000-" + String.format("%04d", i)
            ));
        }

        List<Store> savedStores = storeRepository.saveAll(stores);

        List<Party> parties = new ArrayList<>();
        for (Store store : savedStores) {
            for (int i = 0; i < PARTIES_PER_STORE; i++) {
                parties.add(new Party(
                        store.getName() + " 파티 " + i,
                        "테스트 상품 " + i,
                        10000 + (i * 1000),
                        store,
                        10 + i,
                        "https://open.kakao.com/o/benchmark"
                ));
            }
        }

        partyRepository.saveAll(parties);

        Store targetStore = savedStores.get(savedStores.size() / 2);
        return new SeedResult(targetStore.getId(), 37.55, 127.05);
    }

    private BenchmarkResult benchmarkNearbyStores(double latitude, double longitude) throws Exception {
        for (int i = 0; i < WARM_UP_RUNS; i++) {
            callNearbyStores(latitude, longitude);
        }

        List<Long> latencies = new ArrayList<>();
        int resultSize = 0;
        for (int i = 0; i < MEASURED_RUNS; i++) {
            long startedAt = System.nanoTime();
            MvcResult result = callNearbyStores(latitude, longitude);
            latencies.add(System.nanoTime() - startedAt);
            resultSize = objectMapper.readTree(result.getResponse().getContentAsString()).size();
        }

        return BenchmarkResult.from(latencies, resultSize);
    }

    private BenchmarkResult benchmarkStoreParties(Long storeId) throws Exception {
        for (int i = 0; i < WARM_UP_RUNS; i++) {
            callStoreParties(storeId);
        }

        List<Long> latencies = new ArrayList<>();
        int resultSize = 0;
        for (int i = 0; i < MEASURED_RUNS; i++) {
            long startedAt = System.nanoTime();
            MvcResult result = callStoreParties(storeId);
            latencies.add(System.nanoTime() - startedAt);
            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            resultSize = response.size();
        }

        return BenchmarkResult.from(latencies, resultSize);
    }

    private MvcResult callNearbyStores(double latitude, double longitude) throws Exception {
        return mockMvc.perform(get("/api/stores/nearby")
                        .param("latitude", String.valueOf(latitude))
                        .param("longitude", String.valueOf(longitude)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private MvcResult callStoreParties(Long storeId) throws Exception {
        return mockMvc.perform(get("/api/stores/{storeId}/parties", storeId))
                .andExpect(status().isOk())
                .andReturn();
    }

    private record SeedResult(Long targetStoreId, double referenceLatitude, double referenceLongitude) {
    }

    private record BenchmarkResult(double averageMillis, double p95Millis, int resultSize) {
        private static BenchmarkResult from(List<Long> latencies, int resultSize) {
            double averageMillis = latencies.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0) / 1_000_000.0;

            List<Long> sorted = latencies.stream()
                    .sorted(Comparator.naturalOrder())
                    .toList();

            int p95Index = Math.max(0, (int) Math.ceil(sorted.size() * 0.95) - 1);
            double p95Millis = sorted.get(p95Index) / 1_000_000.0;

            return new BenchmarkResult(averageMillis, p95Millis, resultSize);
        }
    }
}
