package com.project.partition_mate.repository;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.domain.WaitingQueueEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class WaitingQueueRepositoryTest {

    @Autowired
    private WaitingQueueRepository waitingQueueRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("같은 파티에 같은 사용자를 같은 상태의 대기열로 두 번 저장하면 예외가 발생한다")
    void 같은_파티에_같은_사용자를_같은_상태의_대기열로_두번_저장하면_예외가_발생한다() {
        // given
        Store store = storeRepository.save(new Store(
                "트레이더스 구성점",
                "경기 용인시",
                LocalTime.of(10, 0),
                LocalTime.of(22, 0),
                37.3,
                127.1,
                "031-0000-0000"
        ));
        User user = userRepository.save(new User("waiter", "waiter@test.com", "pw", "용인", 37.3, 127.1));
        Party party = partyRepository.save(new Party("베이글 소분", "베이글", 18000, store, 3, "https://open.kakao.com/o/test"));

        waitingQueueRepository.saveAndFlush(WaitingQueueEntry.create(party, user, 1));

        // when
        // then
        assertThatThrownBy(() -> waitingQueueRepository.saveAndFlush(WaitingQueueEntry.create(party, user, 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
