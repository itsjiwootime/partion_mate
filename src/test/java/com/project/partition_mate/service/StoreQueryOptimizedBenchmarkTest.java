package com.project.partition_mate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.repository.PartyRepository;
import com.project.partition_mate.repository.StoreRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
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
class StoreQueryOptimizedBenchmarkTest {

    private static final int STORE_COUNT = 300;
    private static final int PARTIES_PER_STORE = 6;
    private static final int MEASURED_RUNS = 20;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private StoreQueryCacheSupport storeQueryCacheSupport;

    @Autowired
    private CacheManager cacheManager;

    @AfterEach
    void tearDown() {
        storeQueryCacheSupport.clearAll();
        partyRepository.deleteAll();
        storeRepository.deleteAll();
    }

    @Test
    void 최적화된_지점과_파티_조회_성능을_기록한다() throws Exception {
        // given
        SeedResult seedResult = seedStoresAndParties();

        // when
        BenchmarkResult nearbyCold = benchmarkNearbyStoresCold(seedResult.referenceLatitude(), seedResult.referenceLongitude());
        CachedBenchmarkResult nearbyWarm = benchmarkNearbyStoresWarm(seedResult.referenceLatitude(), seedResult.referenceLongitude());
        BenchmarkResult storePartiesCold = benchmarkStorePartiesCold(seedResult.targetStoreId());
        CachedBenchmarkResult storePartiesWarm = benchmarkStorePartiesWarm(seedResult.targetStoreId());

        // then
        assertThat(nearbyCold.resultSize()).isEqualTo(STORE_COUNT);
        assertThat(nearbyWarm.resultSize()).isEqualTo(STORE_COUNT);
        assertThat(storePartiesCold.resultSize()).isEqualTo(PARTIES_PER_STORE);
        assertThat(storePartiesWarm.resultSize()).isEqualTo(PARTIES_PER_STORE);
        assertThat(nearbyWarm.hitRate()).isGreaterThan(0.0);
        assertThat(storePartiesWarm.hitRate()).isGreaterThan(0.0);

        System.out.printf(
                "STORE_QUERY_OPTIMIZED nearbyColdAvgMs=%.2f nearbyColdP95Ms=%.2f nearbyWarmAvgMs=%.2f nearbyWarmP95Ms=%.2f nearbyHitRate=%.2f nearbyCount=%d storePartiesColdAvgMs=%.2f storePartiesColdP95Ms=%.2f storePartiesWarmAvgMs=%.2f storePartiesWarmP95Ms=%.2f storePartiesHitRate=%.2f storePartiesCount=%d%n",
                nearbyCold.averageMillis(),
                nearbyCold.p95Millis(),
                nearbyWarm.averageMillis(),
                nearbyWarm.p95Millis(),
                nearbyWarm.hitRate(),
                nearbyWarm.resultSize(),
                storePartiesCold.averageMillis(),
                storePartiesCold.p95Millis(),
                storePartiesWarm.averageMillis(),
                storePartiesWarm.p95Millis(),
                storePartiesWarm.hitRate(),
                storePartiesWarm.resultSize()
        );
    }

    private SeedResult seedStoresAndParties() {
        List<Store> stores = new ArrayList<>();

        for (int i = 0; i < STORE_COUNT; i++) {
            stores.add(new Store(
                    "최적화 벤치마크 지점 " + i,
                    "서울시 벤치마크로 " + i,
                    LocalTime.of(10, 0),
                    LocalTime.of(22, 0),
                    37.40 + (i * 0.001),
                    127.00 + (i * 0.001),
                    "02-1000-" + String.format("%04d", i)
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

    private BenchmarkResult benchmarkNearbyStoresCold(double latitude, double longitude) throws Exception {
        List<Long> latencies = new ArrayList<>();
        int resultSize = 0;

        for (int i = 0; i < MEASURED_RUNS; i++) {
            storeQueryCacheSupport.clearNearbyStores();

            long startedAt = System.nanoTime();
            MvcResult result = callNearbyStores(latitude, longitude);
            latencies.add(System.nanoTime() - startedAt);
            resultSize = objectMapper.readTree(result.getResponse().getContentAsString()).size();
        }

        return BenchmarkResult.from(latencies, resultSize);
    }

    private CachedBenchmarkResult benchmarkNearbyStoresWarm(double latitude, double longitude) throws Exception {
        storeQueryCacheSupport.clearNearbyStores();
        CacheStatsSnapshot before = cacheStats(StoreQueryCacheSupport.NEARBY_STORES_CACHE);

        List<Long> latencies = new ArrayList<>();
        int resultSize = 0;
        for (int i = 0; i < MEASURED_RUNS; i++) {
            long startedAt = System.nanoTime();
            MvcResult result = callNearbyStores(latitude, longitude);
            latencies.add(System.nanoTime() - startedAt);
            resultSize = objectMapper.readTree(result.getResponse().getContentAsString()).size();
        }

        CacheStatsSnapshot after = cacheStats(StoreQueryCacheSupport.NEARBY_STORES_CACHE);
        return CachedBenchmarkResult.from(latencies, resultSize, before, after);
    }

    private BenchmarkResult benchmarkStorePartiesCold(Long storeId) throws Exception {
        List<Long> latencies = new ArrayList<>();
        int resultSize = 0;

        for (int i = 0; i < MEASURED_RUNS; i++) {
            storeQueryCacheSupport.evictStoreParties(storeId);

            long startedAt = System.nanoTime();
            MvcResult result = callStoreParties(storeId);
            latencies.add(System.nanoTime() - startedAt);
            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            resultSize = response.size();
        }

        return BenchmarkResult.from(latencies, resultSize);
    }

    private CachedBenchmarkResult benchmarkStorePartiesWarm(Long storeId) throws Exception {
        storeQueryCacheSupport.evictStoreParties(storeId);
        CacheStatsSnapshot before = cacheStats(StoreQueryCacheSupport.STORE_PARTIES_CACHE);

        List<Long> latencies = new ArrayList<>();
        int resultSize = 0;
        for (int i = 0; i < MEASURED_RUNS; i++) {
            long startedAt = System.nanoTime();
            MvcResult result = callStoreParties(storeId);
            latencies.add(System.nanoTime() - startedAt);
            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            resultSize = response.size();
        }

        CacheStatsSnapshot after = cacheStats(StoreQueryCacheSupport.STORE_PARTIES_CACHE);
        return CachedBenchmarkResult.from(latencies, resultSize, before, after);
    }

    private CacheStatsSnapshot cacheStats(String cacheName) {
        CaffeineCache cache = (CaffeineCache) cacheManager.getCache(cacheName);
        CacheStats stats = cache != null ? cache.getNativeCache().stats() : CacheStats.empty();
        return new CacheStatsSnapshot(stats.hitCount(), stats.missCount());
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

    private record CacheStatsSnapshot(long hitCount, long missCount) {
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

    private record CachedBenchmarkResult(double averageMillis, double p95Millis, int resultSize, double hitRate) {
        private static CachedBenchmarkResult from(List<Long> latencies,
                                                  int resultSize,
                                                  CacheStatsSnapshot before,
                                                  CacheStatsSnapshot after) {
            BenchmarkResult benchmarkResult = BenchmarkResult.from(latencies, resultSize);
            long hits = Math.max(after.hitCount() - before.hitCount(), 0);
            long misses = Math.max(after.missCount() - before.missCount(), 0);
            long total = hits + misses;
            double hitRate = total == 0 ? 0.0 : (double) hits / total;

            return new CachedBenchmarkResult(
                    benchmarkResult.averageMillis(),
                    benchmarkResult.p95Millis(),
                    benchmarkResult.resultSize(),
                    hitRate
            );
        }
    }
}
