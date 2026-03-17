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
        name = "user_block",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_block_blocker_blocked", columnNames = {"blocker_id", "blocked_id"}),
        indexes = {
                @Index(name = "idx_user_block_blocker_created_at", columnList = "blocker_id, created_at"),
                @Index(name = "idx_user_block_blocked_id", columnList = "blocked_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private UserBlock(User blocker, User blocked, LocalDateTime createdAt) {
        this.blocker = Objects.requireNonNull(blocker, "차단한 사용자는 필수입니다.");
        this.blocked = Objects.requireNonNull(blocked, "차단 대상 사용자는 필수입니다.");
        this.createdAt = Objects.requireNonNull(createdAt, "차단 시각은 필수입니다.");
    }

    public static UserBlock create(User blocker, User blocked, LocalDateTime createdAt) {
        return new UserBlock(blocker, blocked, createdAt);
    }
}
