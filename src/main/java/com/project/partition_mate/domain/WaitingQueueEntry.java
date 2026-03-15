package com.project.partition_mate.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "waiting_queue",
        uniqueConstraints = @UniqueConstraint(columnNames = {"party_id", "user_id", "status"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WaitingQueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer requestedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WaitingQueueStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime queuedAt;

    private WaitingQueueEntry(Party party, User user, Integer requestedQuantity) {
        this.party = Objects.requireNonNull(party, "파티는 필수입니다.");
        this.user = Objects.requireNonNull(user, "사용자는 필수입니다.");
        validateRequestedQuantity(requestedQuantity);
        this.requestedQuantity = requestedQuantity;
        this.status = WaitingQueueStatus.WAITING;
        this.queuedAt = LocalDateTime.now();
    }

    public static WaitingQueueEntry create(Party party, User user, Integer requestedQuantity) {
        return new WaitingQueueEntry(party, user, requestedQuantity);
    }

    public void promote() {
        this.status = WaitingQueueStatus.PROMOTED;
    }

    public void cancel() {
        this.status = WaitingQueueStatus.CANCELED;
    }

    public void expire() {
        this.status = WaitingQueueStatus.EXPIRED;
    }

    public boolean isWaiting() {
        return this.status == WaitingQueueStatus.WAITING;
    }

    private static void validateRequestedQuantity(Integer requestedQuantity) {
        if (requestedQuantity == null) {
            throw new IllegalArgumentException("대기 요청 수량은 필수입니다.");
        }

        if (requestedQuantity <= 0) {
            throw new IllegalArgumentException("대기 요청 수량은 1 이상이어야 합니다.");
        }
    }
}
