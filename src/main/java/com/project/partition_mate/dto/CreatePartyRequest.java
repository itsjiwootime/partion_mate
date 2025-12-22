package com.project.partition_mate.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreatePartyRequest {

    @NotNull(message = "제목은 필수 입니다.")
    String title;

    @NotNull(message = "지점을 선택해 주세요")
    Long storeId;

    @NotNull(message = "제품명을 적어주세요")
    String productName;

    @NotNull
    Integer totalPrice;

    @NotNull
    @Min(value = 1, message = "총 수량은 1개 이상이어야 합니다.")
    Integer totalQuantity;

    @NotNull
    @Min(value = 0, message = "호스트 요청 수량은 0개 이상이어야 합니다.")
    Integer hostRequestedQuantity;

    String openChatUrl;
}
