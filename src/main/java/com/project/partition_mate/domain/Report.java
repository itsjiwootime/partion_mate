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
        name = "report",
        indexes = {
                @Index(name = "idx_report_reporter_created_at", columnList = "reporter_id, created_at"),
                @Index(name = "idx_report_status_target", columnList = "status, target_type")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id")
    private Party party;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private ReportTargetType targetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_type", nullable = false)
    private ReportReasonType reasonType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @Column(length = 1000)
    private String memo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private Report(User reporter,
                   Party party,
                   User targetUser,
                   ReportTargetType targetType,
                   ReportReasonType reasonType,
                   String memo,
                   LocalDateTime createdAt) {
        this.reporter = Objects.requireNonNull(reporter, "신고자는 필수입니다.");
        this.party = party;
        this.targetUser = targetUser;
        this.targetType = Objects.requireNonNull(targetType, "신고 대상 타입은 필수입니다.");
        this.reasonType = Objects.requireNonNull(reasonType, "신고 사유는 필수입니다.");
        this.memo = normalizeMemo(memo);
        this.status = ReportStatus.RECEIVED;
        this.createdAt = Objects.requireNonNull(createdAt, "신고 접수 시각은 필수입니다.");
    }

    public static Report create(User reporter,
                                Party party,
                                User targetUser,
                                ReportTargetType targetType,
                                ReportReasonType reasonType,
                                String memo,
                                LocalDateTime createdAt) {
        return new Report(reporter, party, targetUser, targetType, reasonType, memo, createdAt);
    }

    private String normalizeMemo(String memo) {
        if (memo == null) {
            return null;
        }

        String trimmed = memo.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        if (trimmed.length() > 1000) {
            throw new IllegalArgumentException("신고 메모는 1000자를 초과할 수 없습니다.");
        }
        return trimmed;
    }
}
