package com.project.partition_mate.service;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.domain.PartyMemberRole;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.repository.PartyMemberRepository;
import com.project.partition_mate.repository.PartyRepository;
import com.project.partition_mate.repository.StoreRepository;
import com.project.partition_mate.repository.UserRepository;
import com.project.partition_mate.security.CustomUserDetails;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PartyConcurrencyIntegrationTest {

    @Autowired
    private PartyService partyService;
    @Autowired
    private PartyRepository partyRepository;
    @Autowired
    private PartyMemberRepository partyMemberRepository;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager em;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        partyMemberRepository.deleteAll();
        partyRepository.deleteAll();
        storeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @Transactional
    void 동시에_요청해도_총수량을_초과하지_않는다() throws Exception {
        // given
        Store store = storeRepository.save(new Store(
                "테스트 지점",
                "서울시",
                LocalTime.of(9, 0),
                LocalTime.of(22, 0),
                37.5,
                127.0,
                "02-0000-0000"
        ));
        User host = userRepository.save(new User("host", "host@test.com", "pw", "서울", 37.5, 127.0));
        setAuth(host);

        Party party = new Party("비타민", "비타민B", 35000, store, 3,"url");
        PartyMember hostMember = PartyMember.joinAsHost(party, host, 1);
        party.acceptMember(hostMember);
        partyRepository.save(party);

        int threads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        List<Exception> errors = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            User member = userRepository.save(new User("user" + i, "user" + i + "@t.t", "pw", "주소", 37.0, 127.0));
            executor.submit(() -> {
                try {
                    setAuth(member);
                    partyService.joinParty(party.getId(), new com.project.partition_mate.dto.JoinPartyRequest() {{
                        // reflection 없이 세터가 없으므로 수량만 디폴트 1로 사용
                    }});
                } catch (Exception e) {
                    errors.add(e);
                } finally {
                    SecurityContextHolder.clearContext();
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        em.flush();
        em.clear();

        Party reloaded = partyRepository.findById(party.getId()).orElseThrow();
        assertThat(reloaded.getRequestedQuantity()).isLessThanOrEqualTo(reloaded.getTotalQuantity());
    }

    private void setAuth(User user) {
        CustomUserDetails principal = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
