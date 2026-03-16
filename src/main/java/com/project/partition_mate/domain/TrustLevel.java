package com.project.partition_mate.domain;

import lombok.Getter;

@Getter
public enum TrustLevel {
    NEW("신규"),
    TOP("매우 신뢰"),
    GOOD("신뢰"),
    NORMAL("보통"),
    CAUTION("주의");

    private final String label;

    TrustLevel(String label) {
        this.label = label;
    }
}
