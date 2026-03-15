package com.project.partition_mate.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Table(
        name = "partymember",
        uniqueConstraints = @UniqueConstraint(columnNames = {"party_id", "user_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartyMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id")
    private Party party;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private PartyMemberRole role;

    @Column(nullable = false)
    private Integer requestedQuantity;

    @Builder
    public PartyMember(Party party, User user,PartyMemberRole role, Integer requestedQuantity) {

        this.party = Objects.requireNonNull(party,"파티를 선택해주세요");
        this.user = Objects.requireNonNull(user,"사용자는 필수입니다.");
        this.role = Objects.requireNonNull(role, "롤을 정해주세요");
        validateQuantity(requestedQuantity);
        this.requestedQuantity = requestedQuantity;

    }

    public static PartyMember joinAsHost(Party party, User user, Integer requestedQuantity) {
        validateQuantity(requestedQuantity);
        return new PartyMember(party, user, PartyMemberRole.LEADER, requestedQuantity);
    }

    public static PartyMember joinAsMember(Party party, User user, Integer requestedQuantity) {
        validateMemberQuantity(requestedQuantity);
        return new PartyMember(party, user, PartyMemberRole.MEMBER, requestedQuantity);
    }

    public boolean isHost() {
        return this.role == PartyMemberRole.LEADER;
    }

    public boolean isMember() {
        return this.role == PartyMemberRole.MEMBER;
    }

    private static void validateQuantity(Integer quantity) {
        if (quantity == null) {
            throw new IllegalArgumentException("요청 수량은 필수입니다.");
        }

        if (quantity < 0) {
            throw new IllegalArgumentException("요청 수량은 0 이상이어야 합니다.");
        }
    }

    private static void validateMemberQuantity(Integer quantity) {
        validateQuantity(quantity);

        if (quantity == 0) {
            throw new IllegalArgumentException("참여 요청 수량은 1 이상이어야 합니다.");
        }
    }
}
