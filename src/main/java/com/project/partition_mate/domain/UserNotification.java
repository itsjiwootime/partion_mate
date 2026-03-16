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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "user_notification",
        indexes = {
                @Index(name = "idx_notification_user_created_at", columnList = "user_id, created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserNotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String message;

    private String linkUrl;

    @Column(name = "external_key", nullable = false, unique = true)
    private String externalKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    private UserNotification(User user,
                             UserNotificationType type,
                             String title,
                             String message,
                             String linkUrl,
                             String externalKey,
                             LocalDateTime createdAt) {
        this.user = Objects.requireNonNull(user, "알림 대상 사용자는 필수입니다.");
        this.type = Objects.requireNonNull(type, "알림 타입은 필수입니다.");
        this.title = Objects.requireNonNull(title, "알림 제목은 필수입니다.");
        this.message = Objects.requireNonNull(message, "알림 본문은 필수입니다.");
        this.linkUrl = linkUrl;
        this.externalKey = Objects.requireNonNull(externalKey, "externalKey는 필수입니다.");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt은 필수입니다.");
    }

    public static UserNotification create(User user,
                                          UserNotificationType type,
                                          String title,
                                          String message,
                                          String linkUrl,
                                          String externalKey,
                                          LocalDateTime createdAt) {
        return new UserNotification(user, type, title, message, linkUrl, externalKey, createdAt);
    }

    public boolean isRead() {
        return this.readAt != null;
    }
}
