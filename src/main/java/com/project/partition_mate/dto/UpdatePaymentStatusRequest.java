package com.project.partition_mate.dto;

import com.project.partition_mate.domain.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class UpdatePaymentStatusRequest {

    @NotNull(message = "송금 상태는 필수입니다.")
    private PaymentStatus paymentStatus;
}
