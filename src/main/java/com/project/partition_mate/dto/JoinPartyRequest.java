package com.project.partition_mate.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class JoinPartyRequest {

    @Min(value = 1, message = "요청 수량은 최소 1개 이상이어야 합니다.")
    private Integer memberRequestQuantity;


}
