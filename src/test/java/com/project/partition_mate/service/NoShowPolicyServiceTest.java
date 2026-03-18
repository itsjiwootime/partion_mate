package com.project.partition_mate.service;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.domain.TradeStatus;
import com.project.partition_mate.domain.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NoShowPolicyServiceTest {

    private final NoShowPolicyService noShowPolicyService = new NoShowPolicyService(null);

    @Test
    void 최근_확정거래가_노쇼_1회면_경고단계로_계산한다() {
        // given
        List<PartyMember> histories = List.of(history(TradeStatus.NO_SHOW));

        // when
        NoShowPolicyService.Decision decision = noShowPolicyService.resolve(histories);

        // then
        assertThat(decision.stage()).isEqualTo(NoShowPolicyService.Stage.WARN);
        assertThat(decision.activeNoShowCount()).isEqualTo(1);
        assertThat(decision.shouldWarn()).isTrue();
        assertThat(decision.shouldBlock()).isFalse();
    }

    @Test
    void 최근_확정거래에_노쇼가_2회_연속이면_차단단계로_계산한다() {
        // given
        List<PartyMember> histories = List.of(
                history(TradeStatus.NO_SHOW),
                history(TradeStatus.NO_SHOW)
        );

        // when
        NoShowPolicyService.Decision decision = noShowPolicyService.resolve(histories);

        // then
        assertThat(decision.stage()).isEqualTo(NoShowPolicyService.Stage.BLOCKED);
        assertThat(decision.activeNoShowCount()).isEqualTo(2);
        assertThat(decision.shouldWarn()).isFalse();
        assertThat(decision.shouldBlock()).isTrue();
    }

    @Test
    void 노쇼_뒤에_거래완료가_있으면_제한을_해제한다() {
        // given
        List<PartyMember> histories = List.of(
                history(TradeStatus.COMPLETED),
                history(TradeStatus.NO_SHOW),
                history(TradeStatus.NO_SHOW)
        );

        // when
        NoShowPolicyService.Decision decision = noShowPolicyService.resolve(histories);

        // then
        assertThat(decision.stage()).isEqualTo(NoShowPolicyService.Stage.CLEAR);
        assertThat(decision.activeNoShowCount()).isZero();
        assertThat(decision.shouldWarn()).isFalse();
        assertThat(decision.shouldBlock()).isFalse();
    }

    private PartyMember history(TradeStatus tradeStatus) {
        Store store = new Store(
                "정책 테스트 지점",
                "서울시",
                LocalTime.of(10, 0),
                LocalTime.of(22, 0),
                37.5,
                127.0,
                "02-0000-0000"
        );
        User member = new User("member", "member@test.com", "pw", "서울", 37.5, 127.0);
        Party party = new Party("정책 테스트 파티", "테스트 상품", 10000, store, 2, "https://open.kakao.com/o/no-show-test");
        PartyMember partyMember = PartyMember.joinAsMember(party, member, 1);
        if (tradeStatus == TradeStatus.COMPLETED) {
            partyMember.completeTrade(LocalDateTime.now());
        } else {
            partyMember.markNoShow(LocalDateTime.now());
        }
        return partyMember;
    }
}
