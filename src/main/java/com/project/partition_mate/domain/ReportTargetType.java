package com.project.partition_mate.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportTargetType {
    PARTY("파티"),
    USER("상대 사용자"),
    CHAT("채팅");

    private final String label;
}
