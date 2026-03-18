package com.project.partition_mate.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "favorite_party",
        uniqueConstraints = @UniqueConstraint(name = "uk_favorite_party_user_party", columnNames = {"user_id", "party_id"}),
        indexes = {
                @Index(name = "idx_favorite_party_user_created_at", columnList = "user_id, created_at"),
                @Index(name = "idx_favorite_party_party_id", columnList = "party_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FavoriteParty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private FavoriteParty(User user, Party party, LocalDateTime createdAt) {
        this.user = Objects.requireNonNull(user, "관심 파티 사용자 정보는 필수입니다.");
        this.party = Objects.requireNonNull(party, "관심 파티 대상은 필수입니다.");
        this.createdAt = Objects.requireNonNull(createdAt, "관심 파티 저장 시각은 필수입니다.");
    }

    public static FavoriteParty create(User user, Party party, LocalDateTime createdAt) {
        return new FavoriteParty(user, party, createdAt);
    }
}
