package com.project.partition_mate.service;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.domain.WaitingQueueStatus;
import com.project.partition_mate.dto.JoinPartyRequest;
import com.project.partition_mate.dto.JoinPartyResponse;
import com.project.partition_mate.repository.PartyMemberRepository;
import com.project.partition_mate.repository.PartyRepository;
import com.project.partition_mate.repository.StoreRepository;
import com.project.partition_mate.repository.UserRepository;
import com.project.partition_mate.repository.WaitingQueueRepository;
import com.project.partition_mate.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PartyJoinLoadBenchmarkTest {

    @Autowired
    private PartyService partyService;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private PartyMemberRepository partyMemberRepository;

    @Autowired
    private WaitingQueueRepository waitingQueueRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        waitingQueueRepository.deleteAll();
        partyMemberRepository.deleteAll();
        partyRepository.deleteAll();
        storeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 동시_참여_100건_부하테스트_결과를_기록한다() throws Exception {
        // given
        Store store = storeRepository.saveAndFlush(new Store(
                "부하테스트 지점",
                "서울시",
                LocalTime.of(9, 0),
                LocalTime.of(22, 0),
                37.5,
                127.0,
                "02-0000-0000"
        ));
        User host = userRepository.saveAndFlush(new User("benchmark-host", "benchmark-host@test.com", "pw", "서울", 37.5, 127.0));
        setAuth(host);

        Party party = new Party("부하 테스트 파티", "대용량 상품", 50000, store, 20, "https://open.kakao.com/o/test");
        PartyMember hostMember = PartyMember.joinAsHost(party, host, 1);
        party.acceptMember(hostMember);
        party = partyRepository.saveAndFlush(party);
        Long partyId = party.getId();
        SecurityContextHolder.clearContext();

        int requestCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(requestCount);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        List<Exception> errors = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger joinedCount = new AtomicInteger();
        AtomicInteger waitingCount = new AtomicInteger();

        for (int i = 0; i < requestCount; i++) {
            User member = userRepository.saveAndFlush(new User("benchmark-user-" + i, "benchmark-user-" + i + "@test.com", "pw", "서울", 37.5, 127.0));
            executor.submit(() -> {
                long startNanos = System.nanoTime();
                try {
                    setAuth(member);
                    JoinPartyResponse response = partyService.joinParty(partyId, joinRequest(1));
                    if (response.isWaiting()) {
                        waitingCount.incrementAndGet();
                    } else {
                        joinedCount.incrementAndGet();
                    }
                } catch (Exception ex) {
                    errors.add(ex);
                } finally {
                    latencies.add(System.nanoTime() - startNanos);
                    SecurityContextHolder.clearContext();
                    latch.countDown();
                }
            });
        }

        // when
        latch.await();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // then
        Party reloaded = partyRepository.findById(partyId).orElseThrow();
        List<PartyMember> joinedMembers = partyMemberRepository.findByParty(reloaded);
        int waitingSize = waitingQueueRepository.findAllByPartyAndStatusOrderByQueuedAtAsc(reloaded, WaitingQueueStatus.WAITING).size();
        double averageMs = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0) / 1_000_000.0;
        double p95Ms = percentileMillis(latencies, 0.95);

        System.out.printf("PARTY_JOIN_BENCHMARK joined=%d waiting=%d averageMs=%.2f p95Ms=%.2f%n",
                joinedCount.get(),
                waitingCount.get(),
                averageMs,
                p95Ms
        );

        assertThat(errors).isEmpty();
        assertThat(joinedCount.get()).isEqualTo(19);
        assertThat(waitingCount.get()).isEqualTo(81);
        assertThat(joinedMembers.stream().mapToInt(PartyMember::getRequestedQuantity).sum()).isEqualTo(reloaded.getTotalQuantity());
        assertThat(waitingSize).isEqualTo(81);
    }

    private JoinPartyRequest joinRequest(int requestedQuantity) {
        JoinPartyRequest request = new JoinPartyRequest();
        ReflectionTestUtils.setField(request, "memberRequestQuantity", requestedQuantity);
        return request;
    }

    private double percentileMillis(List<Long> latencies, double percentile) {
        List<Long> sorted = new ArrayList<>(latencies);
        Collections.sort(sorted);
        int index = (int) Math.ceil(sorted.size() * percentile) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(index) / 1_000_000.0;
    }

    private void setAuth(User user) {
        CustomUserDetails principal = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
