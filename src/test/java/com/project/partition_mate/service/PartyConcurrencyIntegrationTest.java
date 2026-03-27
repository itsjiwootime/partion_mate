package com.project.partition_mate.service;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.domain.PartyStatus;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.JoinPartyRequest;
import com.project.partition_mate.dto.JoinPartyResponse;
import com.project.partition_mate.exception.BusinessException;
import com.project.partition_mate.repository.OutboxEventRepository;
import com.project.partition_mate.repository.PartyMemberRepository;
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
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        outboxEventRepository.deleteAll();
        partyMemberRepository.deleteAll();
        partyRepository.deleteAll();
        storeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 동시에_32건_참여를_요청해도_초과_모집이_발생하지_않는다() throws Exception {
        // given
        Store store = storeRepository.saveAndFlush(new Store(
                "동시성 테스트 지점",
                "서울시",
                LocalTime.of(9, 0),
                LocalTime.of(22, 0),
                37.5,
                127.0,
                "02-0000-0000"
        ));
        User host = userRepository.saveAndFlush(new User("host", "host@test.com", "pw", "서울", 37.5, 127.0));
        setAuth(host);

        Party party = new Party("비타민", "비타민B", 35000, store, 10, "url");
        PartyMember hostMember = PartyMember.joinAsHost(party, host, 1);
        party.acceptMember(hostMember);
        party = partyRepository.saveAndFlush(party);
        Long partyId = party.getId();
        SecurityContextHolder.clearContext();

        int requestCount = 32;
        ExecutorService executor = Executors.newFixedThreadPool(16);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(requestCount);
        List<Exception> unexpectedErrors = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger joinedCount = new AtomicInteger();
        AtomicInteger insufficientRejectedCount = new AtomicInteger();

        for (int i = 0; i < requestCount; i++) {
            User member = userRepository.saveAndFlush(new User("user" + i, "user" + i + "@t.t", "pw", "주소", 37.0, 127.0));
            executor.submit(() -> {
                try {
                    startLatch.await();
                    setAuth(member);
                    JoinPartyResponse response = partyService.joinParty(partyId, joinRequest(1));
                    if (response.getJoinStatus() == com.project.partition_mate.domain.ParticipationStatus.JOINED) {
                        joinedCount.incrementAndGet();
                    }
                } catch (BusinessException ex) {
                    if (ex.getMessage().startsWith("남은 수량(")) {
                        insufficientRejectedCount.incrementAndGet();
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
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // then
        Party reloaded = partyRepository.findById(partyId).orElseThrow();
        List<PartyMember> joinedMembers = partyMemberRepository.findByParty(reloaded);
        int requestedQuantity = joinedMembers.stream()
                .mapToInt(PartyMember::getRequestedQuantity)
                .sum();

        assertThat(unexpectedErrors).isEmpty();
        assertThat(joinedCount.get()).isEqualTo(9);
        assertThat(insufficientRejectedCount.get()).isEqualTo(23);
        assertThat(requestedQuantity).isEqualTo(reloaded.getTotalQuantity());
        assertThat(reloaded.getPartyStatus()).isEqualTo(PartyStatus.FULL);
        assertThat(joinedMembers).hasSize(10);
        assertThat(joinedMembers.stream().map(pm -> pm.getUser().getId()).collect(Collectors.toSet()))
                .hasSize(joinedMembers.size());
    }

    @Test
    void 동일_사용자_중복_요청이_포함된_동시_참여_30건에서도_중복_참여가_발생하지_않는다() throws Exception {
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

        Party party = new Party("중복 요청 테스트 파티", "비타민B", 35000, store, 25, "url");
        PartyMember hostMember = PartyMember.joinAsHost(party, host, 1);
        party.acceptMember(hostMember);
        party = partyRepository.saveAndFlush(party);
        Long partyId = party.getId();
        SecurityContextHolder.clearContext();

        int duplicateRequestCount = 10;
        int uniqueRequestCount = 20;
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
        AtomicInteger duplicateRejectedCount = new AtomicInteger();

        for (User requestUser : requestUsers) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    setAuth(requestUser);
                    JoinPartyResponse response = partyService.joinParty(partyId, joinRequest(1));
                    if (response.getJoinStatus() == com.project.partition_mate.domain.ParticipationStatus.JOINED) {
                        joinedCount.incrementAndGet();
                    }
                } catch (BusinessException ex) {
                    if (requestUser.getId().equals(duplicateUser.getId())
                            && ex.getMessage().equals(BusinessException.alreadyJoined().getMessage())) {
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
        long duplicateJoinedCount = joinedMembers.stream()
                .filter(member -> member.getUser().getId().equals(duplicateUser.getId()))
                .count();
        Set<Long> joinedUserIds = joinedMembers.stream()
                .map(member -> member.getUser().getId())
                .collect(Collectors.toSet());

        assertThat(unexpectedErrors).isEmpty();
        assertThat(joinedCount.get()).isEqualTo(21);
        assertThat(duplicateRejectedCount.get()).isEqualTo(duplicateRequestCount - 1);
        assertThat(joinedMembers.stream().mapToInt(PartyMember::getRequestedQuantity).sum()).isEqualTo(22);
        assertThat(joinedUserIds).hasSize(joinedMembers.size());
        assertThat(duplicateJoinedCount).isEqualTo(1);
    }

    private void setAuth(User user) {
        CustomUserDetails principal = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private JoinPartyRequest joinRequest(int requestedQuantity) {
        JoinPartyRequest request = new JoinPartyRequest();
        ReflectionTestUtils.setField(request, "memberRequestQuantity", requestedQuantity);
        return request;
    }
}
