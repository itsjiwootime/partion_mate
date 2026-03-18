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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "external_notification_delivery",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_external_notification_delivery_key", columnNames = {"delivery_key"})
        },
        indexes = {
                @Index(name = "idx_external_delivery_status_next_attempt", columnList = "status, next_attempt_at"),
                @Index(name = "idx_external_delivery_outbox_event", columnList = "outbox_event_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExternalNotificationDelivery {

    private static final int MAX_ERROR_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "outbox_event_id", nullable = false)
    private Long outboxEventId;

    @Column(name = "user_notification_id", nullable = false)
    private Long userNotificationId;

    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExternalNotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private UserNotificationType notificationType;

    @Column(name = "subscription_id", nullable = false)
    private Long subscriptionId;

    @Column(nullable = false)
    private String endpoint;

    @Column(name = "delivery_key", nullable = false)
    private String deliveryKey;

    @Lob
    @Column(name = "payload_json", nullable = false)
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExternalNotificationDeliveryStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "last_status_code")
    private Integer lastStatusCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_failure_reason")
    private ExternalNotificationFailureReason lastFailureReason;

    @Column(name = "last_error_message", length = MAX_ERROR_LENGTH)
    private String lastErrorMessage;

    private ExternalNotificationDelivery(Long outboxEventId,
                                         Long userNotificationId,
                                         Long recipientUserId,
                                         ExternalNotificationChannel channel,
                                         UserNotificationType notificationType,
                                         Long subscriptionId,
                                         String endpoint,
                                         String deliveryKey,
                                         String payloadJson,
                                         LocalDateTime createdAt) {
        this.outboxEventId = Objects.requireNonNull(outboxEventId, "outboxEventId는 필수입니다.");
        this.userNotificationId = Objects.requireNonNull(userNotificationId, "userNotificationId는 필수입니다.");
        this.recipientUserId = Objects.requireNonNull(recipientUserId, "recipientUserId는 필수입니다.");
        this.channel = Objects.requireNonNull(channel, "channel은 필수입니다.");
        this.notificationType = Objects.requireNonNull(notificationType, "notificationType은 필수입니다.");
        this.subscriptionId = Objects.requireNonNull(subscriptionId, "subscriptionId는 필수입니다.");
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint는 필수입니다.");
        this.deliveryKey = Objects.requireNonNull(deliveryKey, "deliveryKey는 필수입니다.");
        this.payloadJson = Objects.requireNonNull(payloadJson, "payloadJson은 필수입니다.");
        this.status = ExternalNotificationDeliveryStatus.PENDING;
        this.attemptCount = 0;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt은 필수입니다.");
        this.nextAttemptAt = createdAt;
    }

    public static ExternalNotificationDelivery create(Long outboxEventId,
                                                      Long userNotificationId,
                                                      Long recipientUserId,
                                                      ExternalNotificationChannel channel,
                                                      UserNotificationType notificationType,
                                                      Long subscriptionId,
                                                      String endpoint,
                                                      String deliveryKey,
                                                      String payloadJson,
                                                      LocalDateTime createdAt) {
        return new ExternalNotificationDelivery(
                outboxEventId,
                userNotificationId,
                recipientUserId,
                channel,
                notificationType,
                subscriptionId,
                endpoint,
                deliveryKey,
                payloadJson,
                createdAt
        );
    }

    public void markSent(LocalDateTime attemptedAt, int statusCode) {
        this.status = ExternalNotificationDeliveryStatus.SENT;
        this.attemptCount += 1;
        this.lastAttemptAt = Objects.requireNonNull(attemptedAt, "attemptedAt은 필수입니다.");
        this.deliveredAt = attemptedAt;
        this.lastStatusCode = statusCode;
        this.lastFailureReason = null;
        this.lastErrorMessage = null;
        this.nextAttemptAt = attemptedAt;
    }

    public void scheduleRetry(LocalDateTime attemptedAt,
                              LocalDateTime nextAttemptAt,
                              Integer statusCode,
                              ExternalNotificationFailureReason failureReason,
                              String errorMessage) {
        this.status = ExternalNotificationDeliveryStatus.PENDING;
        this.attemptCount += 1;
        this.lastAttemptAt = Objects.requireNonNull(attemptedAt, "attemptedAt은 필수입니다.");
        this.nextAttemptAt = Objects.requireNonNull(nextAttemptAt, "nextAttemptAt은 필수입니다.");
        this.lastStatusCode = statusCode;
        this.lastFailureReason = Objects.requireNonNull(failureReason, "failureReason은 필수입니다.");
        this.lastErrorMessage = abbreviate(errorMessage);
    }

    public void markFailed(LocalDateTime attemptedAt,
                           Integer statusCode,
                           ExternalNotificationFailureReason failureReason,
                           String errorMessage) {
        this.status = ExternalNotificationDeliveryStatus.FAILED;
        this.attemptCount += 1;
        this.lastAttemptAt = Objects.requireNonNull(attemptedAt, "attemptedAt은 필수입니다.");
        this.nextAttemptAt = attemptedAt;
        this.lastStatusCode = statusCode;
        this.lastFailureReason = Objects.requireNonNull(failureReason, "failureReason은 필수입니다.");
        this.lastErrorMessage = abbreviate(errorMessage);
    }

    private String abbreviate(String errorMessage) {
        if (errorMessage == null || errorMessage.length() <= MAX_ERROR_LENGTH) {
            return errorMessage;
        }
        return errorMessage.substring(0, MAX_ERROR_LENGTH);
    }
}
