package com.project.partition_mate.service;

import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.domain.PartyMemberRole;
import com.project.partition_mate.domain.TradeStatus;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.repository.PartyMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NoShowPolicyService {

    private static final Set<TradeStatus> DECIDED_TRADE_STATUSES = EnumSet.of(
            TradeStatus.COMPLETED,
            TradeStatus.NO_SHOW
    );

    private final PartyMemberRepository partyMemberRepository;

    public Decision evaluate(User user) {
        List<PartyMember> histories = partyMemberRepository.findAllForNoShowPolicy(
                user,
                PartyMemberRole.MEMBER,
                DECIDED_TRADE_STATUSES
        );

        return resolve(histories);
    }

    Decision resolve(List<PartyMember> histories) {
        int activeNoShowCount = 0;
        for (PartyMember history : histories) {
            if (history.getTradeStatus() == TradeStatus.COMPLETED) {
                break;
            }
            activeNoShowCount++;
        }

        if (activeNoShowCount >= 2) {
            return Decision.blocked(activeNoShowCount);
        }
        if (activeNoShowCount == 1) {
            return Decision.warn(activeNoShowCount);
        }
        return Decision.clear();
    }

    public record Decision(Stage stage, int activeNoShowCount) {

        public static Decision clear() {
            return new Decision(Stage.CLEAR, 0);
        }

        public static Decision warn(int activeNoShowCount) {
            return new Decision(Stage.WARN, activeNoShowCount);
        }

        public static Decision blocked(int activeNoShowCount) {
            return new Decision(Stage.BLOCKED, activeNoShowCount);
        }

        public boolean shouldWarn() {
            return stage == Stage.WARN;
        }

        public boolean shouldBlock() {
            return stage == Stage.BLOCKED;
        }
    }

    public enum Stage {
        CLEAR,
        WARN,
        BLOCKED
    }
}
