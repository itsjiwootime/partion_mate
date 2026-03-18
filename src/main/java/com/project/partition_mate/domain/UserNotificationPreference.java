package com.project.partition_mate.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "user_notification_preference",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_notification_preference_user_type", columnNames = {"user_id", "notification_type"})
        },
        indexes = {
                @Index(name = "idx_notification_preference_user", columnList = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserNotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private UserNotificationType notificationType;

    @Column(name = "web_push_enabled", nullable = false)
    private boolean webPushEnabled;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private UserNotificationPreference(User user,
                                       UserNotificationType notificationType,
                                       boolean webPushEnabled,
                                       LocalDateTime updatedAt) {
        this.user = Objects.requireNonNull(user, "사용자는 필수입니다.");
        this.notificationType = Objects.requireNonNull(notificationType, "알림 타입은 필수입니다.");
        this.webPushEnabled = webPushEnabled;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt은 필수입니다.");
    }

    public static UserNotificationPreference create(User user,
                                                    UserNotificationType notificationType,
                                                    boolean webPushEnabled,
                                                    LocalDateTime updatedAt) {
        return new UserNotificationPreference(user, notificationType, webPushEnabled, updatedAt);
    }

    public void update(boolean webPushEnabled, LocalDateTime updatedAt) {
        this.webPushEnabled = webPushEnabled;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt은 필수입니다.");
    }
}
