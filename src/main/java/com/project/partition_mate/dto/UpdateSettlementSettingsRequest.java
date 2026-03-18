package com.project.partition_mate.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateSettlementSettingsRequest {

    @Size(max = 300, message = "정산 기본 안내는 300자 이하로 입력해 주세요.")
    private String settlementGuide;
}
