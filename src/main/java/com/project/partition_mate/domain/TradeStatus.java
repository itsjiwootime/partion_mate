package com.project.partition_mate.domain;

import lombok.Getter;

@Getter
public enum TradeStatus {
    PENDING("거래 전"),
    COMPLETED("거래 완료"),
    NO_SHOW("노쇼");

    private final String label;

    TradeStatus(String label) {
        this.label = label;
    }
}
