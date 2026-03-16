package com.project.partition_mate.dto;

import com.project.partition_mate.domain.TradeStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class UpdateTradeStatusRequest {

    @NotNull(message = "거래 상태는 필수입니다.")
    private TradeStatus tradeStatus;
}
