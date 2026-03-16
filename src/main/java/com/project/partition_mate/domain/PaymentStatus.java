package com.project.partition_mate.domain;

import lombok.Getter;

@Getter
public enum PaymentStatus {
    NOT_REQUIRED("정산 대상 아님"),
    PENDING("송금 대기"),
    PAID("송금 완료"),
    CONFIRMED("확인 완료"),
    REFUNDED("환불 완료");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }
}
