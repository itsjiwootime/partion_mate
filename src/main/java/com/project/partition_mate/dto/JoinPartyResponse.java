package com.project.partition_mate.dto;

import com.project.partition_mate.domain.ParticipationStatus;
import com.project.partition_mate.domain.Party;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class JoinPartyResponse {

    private final ParticipationStatus joinStatus;
    private final String message;
    private final PartyResponse party;
    private final Warning warning;

    private JoinPartyResponse(ParticipationStatus joinStatus,
                              String message,
                              PartyResponse party,
                              Warning warning) {
        this.joinStatus = joinStatus;
        this.message = message;
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
                PartyResponse.from(party),
                warning
        );
    }

    @Getter
    @AllArgsConstructor(staticName = "of")
    public static class Warning {
        private final String code;
        private final String message;
        private final Integer activeNoShowCount;
    }
}
