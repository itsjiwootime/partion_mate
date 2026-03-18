package com.project.partition_mate.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "outbox_event",
        indexes = {
                @Index(name = "idx_outbox_status_next_attempt", columnList = "status, next_attempt_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    private static final int MAX_ERROR_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String aggregateType;

    @Column(nullable = false)
    private Long aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxEventType eventType;

    @Lob
    @Column(nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxEventStatus status;

    @Column(nullable = false)
    private int retryCount;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    @Column(length = MAX_ERROR_LENGTH)
    private String lastError;

    private OutboxEvent(String aggregateType,
                        Long aggregateId,
                        OutboxEventType eventType,
                        String payload,
                        LocalDateTime createdAt) {
        this.aggregateType = Objects.requireNonNull(aggregateType, "aggregateType은 필수입니다.");
        this.aggregateId = Objects.requireNonNull(aggregateId, "aggregateId는 필수입니다.");
        this.eventType = Objects.requireNonNull(eventType, "eventType은 필수입니다.");
        this.payload = Objects.requireNonNull(payload, "payload는 필수입니다.");
        this.status = OutboxEventStatus.PENDING;
        this.retryCount = 0;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt은 필수입니다.");
        this.nextAttemptAt = createdAt;
    }

    public static OutboxEvent create(String aggregateType,
                                     Long aggregateId,
                                     OutboxEventType eventType,
                                     String payload,
                                     LocalDateTime createdAt) {
        return new OutboxEvent(aggregateType, aggregateId, eventType, payload, createdAt);
    }

    public void markProcessed(LocalDateTime processedAt) {
        this.status = OutboxEventStatus.PROCESSED;
        this.processedAt = Objects.requireNonNull(processedAt, "processedAt은 필수입니다.");
        this.nextAttemptAt = processedAt;
        this.lastError = null;
    }

    public void scheduleRetry(LocalDateTime nextAttemptAt, String lastError) {
        this.status = OutboxEventStatus.PENDING;
        this.retryCount += 1;
        this.nextAttemptAt = Objects.requireNonNull(nextAttemptAt, "nextAttemptAt은 필수입니다.");
        this.lastError = abbreviate(lastError);
    }

    public void markFailed(LocalDateTime failedAt, String lastError) {
        this.status = OutboxEventStatus.FAILED;
        this.retryCount += 1;
        this.nextAttemptAt = Objects.requireNonNull(failedAt, "failedAt은 필수입니다.");
        this.lastError = abbreviate(lastError);
    }

    private String abbreviate(String errorMessage) {
        if (errorMessage == null || errorMessage.length() <= MAX_ERROR_LENGTH) {
            return errorMessage;
        }
        return errorMessage.substring(0, MAX_ERROR_LENGTH);
    }
}
