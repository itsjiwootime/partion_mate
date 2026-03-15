package com.project.partition_mate.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreatePartyRequest {

    @NotBlank(message = "제목은 필수 입니다.")
    private String title;

    @NotNull(message = "지점을 선택해 주세요")
    private Long storeId;

    @NotBlank(message = "제품명을 적어주세요")
    private String productName;

    @NotNull
    @Min(value = 0, message = "총 가격은 0원 이상이어야 합니다.")
    private Integer totalPrice;

    @NotNull
    @Min(value = 1, message = "총 수량은 1개 이상이어야 합니다.")
    private Integer totalQuantity;

    @NotNull
    @Min(value = 0, message = "호스트 요청 수량은 0개 이상이어야 합니다.")
    private Integer hostRequestedQuantity;

    private String openChatUrl;
}
