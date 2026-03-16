package com.project.partition_mate.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;

@Getter
public class ConfirmSettlementRequest {

    @NotNull(message = "실구매 총액은 필수입니다.")
    @PositiveOrZero(message = "실구매 총액은 0 이상이어야 합니다.")
    private Integer actualTotalPrice;

    private String receiptNote;
}
