package com.project.partition_mate.service;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.domain.WaitingQueueEntry;
import com.project.partition_mate.domain.WaitingQueueStatus;
import com.project.partition_mate.dto.JoinPartyResponse;
import com.project.partition_mate.exception.BusinessException;
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
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PartyConcurrencyIntegrationTest {

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
    void 동시에_요청해도_총수량을_초과하지_않는다() throws Exception {
        // given
        Store store = storeRepository.saveAndFlush(new Store(
                "테스트 지점",
                "서울시",
                LocalTime.of(9, 0),
                LocalTime.of(22, 0),
                37.5,
                127.0,
                "02-0000-0000"
        ));
        User host = userRepository.saveAndFlush(new User("host", "host@test.com", "pw", "서울", 37.5, 127.0));
        setAuth(host);

        Party party = new Party("비타민", "비타민B", 35000, store, 3,"url");
        PartyMember hostMember = PartyMember.joinAsHost(party, host, 1);
        party.acceptMember(hostMember);
        party = partyRepository.saveAndFlush(party);
        Long partyId = party.getId();
        SecurityContextHolder.clearContext();

        int threads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        List<Exception> errors = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger joinedCount = new AtomicInteger();
        AtomicInteger waitingCount = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            User member = userRepository.saveAndFlush(new User("user" + i, "user" + i + "@t.t", "pw", "주소", 37.0, 127.0));
            executor.submit(() -> {
                try {
                    setAuth(member);
                    com.project.partition_mate.dto.JoinPartyRequest request = new com.project.partition_mate.dto.JoinPartyRequest();
                    ReflectionTestUtils.setField(request, "memberRequestQuantity", 1);
                    JoinPartyResponse response = partyService.joinParty(partyId, request);
                    if (response.isWaiting()) {
                        waitingCount.incrementAndGet();
                    } else {
                        joinedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errors.add(e);
                } finally {
                    SecurityContextHolder.clearContext();
                    latch.countDown();
                }
            });
        }

        // when
        latch.await();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // then
        Party reloaded = partyRepository.findById(partyId).orElseThrow();
        List<PartyMember> joinedMembers = partyMemberRepository.findByParty(reloaded);
        int waitingSize = waitingQueueRepository.findAllByPartyAndStatusOrderByQueuedAtAsc(reloaded, WaitingQueueStatus.WAITING).size();
        int requestedQuantity = joinedMembers.stream()
                .mapToInt(PartyMember::getRequestedQuantity)
                .sum();

        assertThat(joinedCount.get()).isEqualTo(2);
        assertThat(waitingCount.get()).isEqualTo(3);
        assertThat(requestedQuantity).isEqualTo(reloaded.getTotalQuantity());
        assertThat(reloaded.getPartyStatus()).isEqualTo(com.project.partition_mate.domain.PartyStatus.FULL);
        assertThat(joinedMembers).hasSize(3);
        assertThat(waitingSize).isEqualTo(3);
        assertThat(errors).isEmpty();
    }

    @Test
    void 동일_사용자_중복_요청이_포함된_동시_참여_100건에서도_중복_참여가_발생하지_않는다() throws Exception {
        // given
        Store store = storeRepository.saveAndFlush(new Store(
                "중복 요청 테스트 지점",
                "서울시",
                LocalTime.of(9, 0),
                LocalTime.of(22, 0),
                37.5,
                127.0,
                "02-0000-0000"
        ));
        User host = userRepository.saveAndFlush(new User("host", "host@test.com", "pw", "서울", 37.5, 127.0));
        setAuth(host);

        Party party = new Party("중복 요청 테스트 파티", "비타민B", 35000, store, 20, "url");
        PartyMember hostMember = PartyMember.joinAsHost(party, host, 1);
        party.acceptMember(hostMember);
        party = partyRepository.saveAndFlush(party);
        Long partyId = party.getId();
        SecurityContextHolder.clearContext();

        int duplicateRequestCount = 10;
        int uniqueRequestCount = 90;
        int totalRequestCount = duplicateRequestCount + uniqueRequestCount;
        User duplicateUser = userRepository.saveAndFlush(new User(
                "duplicate-user",
                "duplicate-user@test.com",
                "pw",
                "서울",
                37.5,
                127.0
        ));

        List<User> requestUsers = new ArrayList<>();
        for (int i = 0; i < uniqueRequestCount; i++) {
            requestUsers.add(userRepository.saveAndFlush(new User(
                    "user" + i,
                    "user" + i + "@test.com",
                    "pw",
                    "서울",
                    37.5,
                    127.0
            )));
        }
        for (int i = 0; i < duplicateRequestCount; i++) {
            requestUsers.add(duplicateUser);
        }
        Collections.shuffle(requestUsers);

        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalRequestCount);
        List<Exception> unexpectedErrors = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger joinedCount = new AtomicInteger();
        AtomicInteger waitingCount = new AtomicInteger();
        AtomicInteger duplicateRejectedCount = new AtomicInteger();

        for (User requestUser : requestUsers) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    setAuth(requestUser);
                    JoinPartyResponse response = partyService.joinParty(partyId, joinRequest(1));
                    if (response.isWaiting()) {
                        waitingCount.incrementAndGet();
                    } else {
                        joinedCount.incrementAndGet();
                    }
                } catch (BusinessException ex) {
                    if (requestUser.getId().equals(duplicateUser.getId())
                            && (ex.getMessage().equals(BusinessException.alreadyJoined().getMessage())
                            || ex.getMessage().equals(BusinessException.alreadyWaiting().getMessage()))) {
                        duplicateRejectedCount.incrementAndGet();
                    } else {
                        unexpectedErrors.add(ex);
                    }
                } catch (Exception ex) {
                    unexpectedErrors.add(ex);
                } finally {
                    SecurityContextHolder.clearContext();
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
        Party reloaded = partyRepository.findById(partyId).orElseThrow();
        List<PartyMember> joinedMembers = partyMemberRepository.findByParty(reloaded);
        List<WaitingQueueEntry> waitingEntries = waitingQueueRepository.findAllByPartyAndStatusOrderByQueuedAtAsc(
                reloaded,
                WaitingQueueStatus.WAITING
        );

        long duplicateJoinedCount = joinedMembers.stream()
                .filter(member -> member.getUser().getId().equals(duplicateUser.getId()))
                .count();
        long duplicateWaitingCount = waitingEntries.stream()
                .filter(entry -> entry.getUser().getId().equals(duplicateUser.getId()))
                .count();

        Set<Long> joinedUserIds = joinedMembers.stream()
                .map(member -> member.getUser().getId())
                .collect(Collectors.toSet());
        Set<Long> waitingUserIds = waitingEntries.stream()
                .map(entry -> entry.getUser().getId())
                .collect(Collectors.toSet());

        assertThat(unexpectedErrors).isEmpty();
        assertThat(duplicateRejectedCount.get()).isEqualTo(duplicateRequestCount - 1);
        assertThat(joinedCount.get() + waitingCount.get() + duplicateRejectedCount.get()).isEqualTo(totalRequestCount);
        assertThat(joinedMembers.stream().mapToInt(PartyMember::getRequestedQuantity).sum()).isEqualTo(reloaded.getTotalQuantity());
        assertThat(joinedUserIds).hasSize(joinedMembers.size());
        assertThat(waitingUserIds).hasSize(waitingEntries.size());
        assertThat(joinedUserIds).doesNotContainAnyElementsOf(waitingUserIds);
        assertThat(duplicateJoinedCount + duplicateWaitingCount).isEqualTo(1);
    }

    private void setAuth(User user) {
        CustomUserDetails principal = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private com.project.partition_mate.dto.JoinPartyRequest joinRequest(int requestedQuantity) {
        com.project.partition_mate.dto.JoinPartyRequest request = new com.project.partition_mate.dto.JoinPartyRequest();
        ReflectionTestUtils.setField(request, "memberRequestQuantity", requestedQuantity);
        return request;
    }
}
