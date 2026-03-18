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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "web_push_subscription",
        indexes = {
                @Index(name = "idx_web_push_subscription_user_updated_at", columnList = "user_id, updated_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WebPushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 1000)
    private String endpoint;

    @Column(nullable = false, length = 512)
    private String p256dh;

    @Column(nullable = false, length = 255)
    private String auth;

    @Column(length = 500)
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private WebPushSubscription(User user,
                                String endpoint,
                                String p256dh,
                                String auth,
                                String userAgent,
                                LocalDateTime createdAt,
                                LocalDateTime updatedAt) {
        this.user = Objects.requireNonNull(user, "구독 사용자 정보는 필수입니다.");
        this.endpoint = requireText(endpoint, "endpoint는 필수입니다.");
        this.p256dh = requireText(p256dh, "p256dh는 필수입니다.");
        this.auth = requireText(auth, "auth는 필수입니다.");
        this.userAgent = userAgent;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt은 필수입니다.");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt은 필수입니다.");
    }

    public static WebPushSubscription create(User user,
                                             String endpoint,
                                             String p256dh,
                                             String auth,
                                             String userAgent,
                                             LocalDateTime now) {
        return new WebPushSubscription(user, endpoint, p256dh, auth, userAgent, now, now);
    }

    public void refresh(User user,
                        String p256dh,
                        String auth,
                        String userAgent,
                        LocalDateTime updatedAt) {
        this.user = Objects.requireNonNull(user, "구독 사용자 정보는 필수입니다.");
        this.p256dh = requireText(p256dh, "p256dh는 필수입니다.");
        this.auth = requireText(auth, "auth는 필수입니다.");
        this.userAgent = userAgent;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt은 필수입니다.");
    }

    public boolean ownedBy(Long userId) {
        return this.user != null
                && this.user.getId() != null
                && Objects.equals(this.user.getId(), userId);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
