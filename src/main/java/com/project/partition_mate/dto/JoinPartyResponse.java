package com.project.partition_mate.dto;

import com.project.partition_mate.domain.ParticipationStatus;
import com.project.partition_mate.domain.Party;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class JoinPartyResponse {

    private final ParticipationStatus joinStatus;
    private final String message;
    private final Integer waitingPosition;
    private final PartyResponse party;
    private final Warning warning;

    private JoinPartyResponse(ParticipationStatus joinStatus,
                              String message,
                              Integer waitingPosition,
                              PartyResponse party,
                              Warning warning) {
        this.joinStatus = joinStatus;
        this.message = message;
        this.waitingPosition = waitingPosition;
        this.party = party;
        this.warning = warning;
    }

    public static JoinPartyResponse joined(Party party) {
        return joined(party, null);
    }

    public static JoinPartyResponse joined(Party party, Warning warning) {
        return new JoinPartyResponse(
                ParticipationStatus.JOINED,
                "파티 참여가 완료되었습니다.",
                null,
                PartyResponse.from(party),
                warning
        );
    }

    public static JoinPartyResponse waiting(Party party, int waitingPosition) {
        return waiting(party, waitingPosition, null);
    }

    public static JoinPartyResponse waiting(Party party, int waitingPosition, Warning warning) {
        return new JoinPartyResponse(
                ParticipationStatus.WAITING,
                "잔여 수량이 부족해 대기열 " + waitingPosition + "번으로 등록되었습니다.",
                waitingPosition,
                PartyResponse.from(party),
                warning
        );
    }

    public boolean isWaiting() {
        return this.joinStatus == ParticipationStatus.WAITING;
    }

    @Getter
    @AllArgsConstructor(staticName = "of")
    public static class Warning {
        private final String code;
        private final String message;
        private final Integer activeNoShowCount;
    }
}
