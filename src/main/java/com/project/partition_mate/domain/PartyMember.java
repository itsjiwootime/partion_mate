package com.project.partition_mate.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Table(name = "partymember")
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
        this.user = Objects.requireNonNull(user,"");
        this.role = Objects.requireNonNull(role, "롤을 정해주세요");
        this.requestedQuantity = requestedQuantity;

    }

    public static PartyMember joinAsHost(Party party, User user, Integer requestedQuantity) {
        return new PartyMember(party, user, PartyMemberRole.LEADER, requestedQuantity);
    }

    public static PartyMember joinAsMember(Party party, User user, Integer requestedQuantity) {
        return new PartyMember(party, user, PartyMemberRole.MEMBER, requestedQuantity);
    }

    public boolean isHost() {
        return this.role == PartyMemberRole.LEADER;
    }

    public boolean isMember() {
        return this.role == PartyMemberRole.MEMBER;
    }

    private void validateQuantity(Integer quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("요청 수량은 0 이상이어야 합니다.");
        }
    }
}
