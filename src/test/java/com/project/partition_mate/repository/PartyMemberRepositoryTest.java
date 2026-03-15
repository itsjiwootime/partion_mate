package com.project.partition_mate.repository;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.domain.User;
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
class PartyMemberRepositoryTest {

    @Autowired
    private PartyMemberRepository partyMemberRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("같은 파티에 같은 사용자를 두 번 저장하면 예외가 발생한다")
    void 같은_파티에_같은_사용자를_두번_저장하면_예외가_발생한다() {
        // given
        Store store = storeRepository.save(new Store(
                "코스트코 양재점",
                "서울 서초구",
                LocalTime.of(10, 0),
                LocalTime.of(22, 0),
                37.0,
                127.0,
                "02-0000-0000"
        ));
        User user = userRepository.save(new User("member", "member@test.com", "pw", "서울", 37.0, 127.0));
        Party party = partyRepository.save(new Party("비타민 소분", "비타민", 30000, store, 4, "https://open.kakao.com/o/test"));

        partyMemberRepository.saveAndFlush(PartyMember.joinAsMember(party, user, 1));

        // when
        // then
        assertThatThrownBy(() -> partyMemberRepository.saveAndFlush(PartyMember.joinAsMember(party, user, 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
