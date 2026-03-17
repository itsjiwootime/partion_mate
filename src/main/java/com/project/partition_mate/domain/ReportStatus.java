package com.project.partition_mate.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportStatus {
    RECEIVED("접수됨");

    private final String label;
}
