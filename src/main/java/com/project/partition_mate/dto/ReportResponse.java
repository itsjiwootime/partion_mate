package com.project.partition_mate.dto;

import com.project.partition_mate.domain.Report;
import com.project.partition_mate.domain.ReportReasonType;
import com.project.partition_mate.domain.ReportStatus;
import com.project.partition_mate.domain.ReportTargetType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ReportResponse {

    private final Long id;
    private final ReportTargetType targetType;
    private final String targetTypeLabel;
    private final Long partyId;
    private final String partyTitle;
    private final Long targetUserId;
    private final String targetUsername;
    private final ReportReasonType reasonType;
    private final String reasonTypeLabel;
    private final ReportStatus status;
    private final String statusLabel;
    private final String memo;
    private final LocalDateTime createdAt;
    private final String createdAtLabel;

    private ReportResponse(Long id,
                           ReportTargetType targetType,
                           Long partyId,
                           String partyTitle,
                           Long targetUserId,
                           String targetUsername,
                           ReportReasonType reasonType,
                           ReportStatus status,
                           String memo,
                           LocalDateTime createdAt) {
        this.id = id;
        this.targetType = targetType;
        this.targetTypeLabel = targetType != null ? targetType.getLabel() : null;
        this.partyId = partyId;
        this.partyTitle = partyTitle;
        this.targetUserId = targetUserId;
        this.targetUsername = targetUsername;
        this.reasonType = reasonType;
        this.reasonTypeLabel = reasonType != null ? reasonType.getLabel() : null;
        this.status = status;
        this.statusLabel = status != null ? status.getLabel() : null;
        this.memo = memo;
        this.createdAt = createdAt;
        this.createdAtLabel = DateTimeLabelFormatter.format(createdAt);
    }

    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getTargetType(),
                report.getParty() != null ? report.getParty().getId() : null,
                report.getParty() != null ? report.getParty().getTitle() : null,
                report.getTargetUser() != null ? report.getTargetUser().getId() : null,
                report.getTargetUser() != null ? report.getTargetUser().getUsername() : null,
                report.getReasonType(),
                report.getStatus(),
                report.getMemo(),
                report.getCreatedAt()
        );
    }
}
