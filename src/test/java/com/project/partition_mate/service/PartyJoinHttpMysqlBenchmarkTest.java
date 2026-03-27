package com.project.partition_mate.service;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.repository.PartyMemberRepository;
import com.project.partition_mate.repository.PartyRepository;
import com.project.partition_mate.repository.StoreRepository;
import com.project.partition_mate.repository.UserRepository;
import com.project.partition_mate.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_HTTP_BENCHMARK", matches = "true")
class PartyJoinHttpMysqlBenchmarkTest {

    private static final String JOIN_REQUEST_BODY = """
            {
              "memberRequestQuantity": 1
            }
            """;

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private PartyMemberRepository partyMemberRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void HTTP와_MySQL_기준_동시_참여_300건_부하테스트_결과를_기록한다() throws Exception {
        // given
        Store store = storeRepository.saveAndFlush(new Store(
                "HTTP 벤치마크 지점",
                "서울시",
                LocalTime.of(9, 0),
                LocalTime.of(22, 0),
                37.5,
                127.0,
                "02-0000-0000"
        ));
        User host = userRepository.saveAndFlush(new User("http-benchmark-host", "http-benchmark-host@test.com", "pw", "서울", 37.5, 127.0));
        Party party = new Party("HTTP 부하 테스트 파티", "대용량 상품", 50000, store, 20, "https://open.kakao.com/o/test");
        party.acceptMember(PartyMember.joinAsHost(party, host, 1));
        party = partyRepository.saveAndFlush(party);

        int requestCount = 300;
        ExecutorService executor = Executors.newFixedThreadPool(64);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(requestCount);
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        ConcurrentLinkedQueue<String> unexpectedResponses = new ConcurrentLinkedQueue<>();
        AtomicInteger joinedCount = new AtomicInteger();
        AtomicInteger insufficientRejectedCount = new AtomicInteger();
        List<String> accessTokens = new ArrayList<>();

        for (int i = 0; i < requestCount; i++) {
            User member = userRepository.saveAndFlush(new User(
                    "http-benchmark-user-" + i,
                    "http-benchmark-user-" + i + "@test.com",
                    "pw",
                    "서울",
                    37.5,
                    127.0
            ));
            accessTokens.add(jwtTokenProvider.createToken(member.getId()));
        }

        URI joinUri = URI.create("http://localhost:" + port + "/party/" + party.getId() + "/join");
        for (String accessToken : accessTokens) {
            executor.submit(() -> {
                long startNanos = System.nanoTime();
                try {
                    startLatch.await();
                    HttpRequest request = HttpRequest.newBuilder(joinUri)
                            .header("Authorization", "Bearer " + accessToken)
                            .header("Content-Type", "application/json")
                            .timeout(Duration.ofSeconds(10))
                            .POST(HttpRequest.BodyPublishers.ofString(JOIN_REQUEST_BODY))
                            .build();
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 201 && response.body().contains("\"joinStatus\":\"JOINED\"")) {
                        joinedCount.incrementAndGet();
                    } else if (response.statusCode() == 409 && response.body().contains("남은 수량(")) {
                        insufficientRejectedCount.incrementAndGet();
                    } else {
                        unexpectedResponses.add(response.statusCode() + " " + response.body());
                    }
                } catch (Exception ex) {
                    unexpectedResponses.add(ex.getClass().getSimpleName() + ": " + ex.getMessage());
                } finally {
                    latencies.add(System.nanoTime() - startNanos);
                    doneLatch.countDown();
                }
            });
        }

        // when
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // then
        Party reloaded = partyRepository.findById(party.getId()).orElseThrow();
        List<PartyMember> joinedMembers = partyMemberRepository.findByParty(reloaded);
        double averageMs = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0) / 1_000_000.0;
        double p95Ms = percentileMillis(latencies, 0.95);
        double p99Ms = percentileMillis(latencies, 0.99);
        Set<Long> joinedUserIds = joinedMembers.stream()
                .map(member -> member.getUser().getId())
                .collect(Collectors.toSet());

        System.out.printf(
                "PARTY_JOIN_HTTP_MYSQL_BENCHMARK joined=%d rejected=%d averageMs=%.2f p95Ms=%.2f p99Ms=%.2f%n",
                joinedCount.get(),
                insufficientRejectedCount.get(),
                averageMs,
                p95Ms,
                p99Ms
        );

        assertThat(unexpectedResponses).isEmpty();
        assertThat(joinedCount.get()).isEqualTo(19);
        assertThat(insufficientRejectedCount.get()).isEqualTo(281);
        assertThat(joinedMembers.stream().mapToInt(PartyMember::getRequestedQuantity).sum()).isEqualTo(reloaded.getTotalQuantity());
        assertThat(joinedUserIds).hasSize(joinedMembers.size());
    }

    private double percentileMillis(List<Long> latencies, double percentile) {
        List<Long> sorted = new ArrayList<>(latencies);
        Collections.sort(sorted);
        int index = (int) Math.ceil(sorted.size() * percentile) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(index) / 1_000_000.0;
    }
}
