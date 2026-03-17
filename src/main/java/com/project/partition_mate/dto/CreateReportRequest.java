package com.project.partition_mate.dto;

import com.project.partition_mate.domain.ReportReasonType;
import com.project.partition_mate.domain.ReportTargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateReportRequest {

    @NotNull(message = "신고 대상 타입을 선택해 주세요.")
    private ReportTargetType targetType;

    private Long partyId;

    private Long targetUserId;

    @NotNull(message = "신고 사유를 선택해 주세요.")
    private ReportReasonType reasonType;

    @Size(max = 1000, message = "신고 메모는 1000자를 초과할 수 없습니다.")
    private String memo;
}
