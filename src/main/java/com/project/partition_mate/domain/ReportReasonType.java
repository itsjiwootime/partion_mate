package com.project.partition_mate.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportReasonType {
    NO_SHOW("노쇼"),
    PAYMENT_ISSUE("정산 문제"),
    INAPPROPRIATE_CHAT("부적절한 채팅"),
    FRAUD_SUSPECTED("사기 의심"),
    SPAM("스팸"),
    OTHER("기타");

    private final String label;
}
