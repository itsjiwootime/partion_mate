package com.project.partition_mate.dto;

import com.project.partition_mate.domain.PackagingType;
import com.project.partition_mate.domain.StorageType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class UpdatePartyRequest {

    @NotBlank(message = "제목은 필수 입니다.")
    private String title;

    @NotBlank(message = "제품명을 적어주세요")
    private String productName;

    @NotNull
    @Min(value = 0, message = "총 가격은 0원 이상이어야 합니다.")
    private Integer totalPrice;

    @NotNull
    @Min(value = 1, message = "총 수량은 1개 이상이어야 합니다.")
    private Integer totalQuantity;

    private String openChatUrl;

    @Future(message = "마감 시간은 현재보다 미래여야 합니다.")
    private LocalDateTime deadline;

    @NotBlank(message = "소분 단위 표기를 입력해 주세요.")
    private String unitLabel;

    @NotNull(message = "최소 소분 단위를 입력해 주세요.")
    @Min(value = 1, message = "최소 소분 단위는 1 이상이어야 합니다.")
    private Integer minimumShareUnit;

    @NotNull(message = "보관 방식을 선택해 주세요.")
    private StorageType storageType;

    @NotNull(message = "포장 방식을 선택해 주세요.")
    private PackagingType packagingType;

    @NotNull(message = "포장 제공 여부를 선택해 주세요.")
    private Boolean hostProvidesPackaging;

    @NotNull(message = "현장 소분 여부를 선택해 주세요.")
    private Boolean onSiteSplit;

    private String guideNote;
}
