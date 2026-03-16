package com.project.partition_mate.domain;

import lombok.Getter;

@Getter
public enum StorageType {
    ROOM_TEMPERATURE("상온"),
    REFRIGERATED("냉장"),
    FROZEN("냉동");

    private final String label;

    StorageType(String label) {
        this.label = label;
    }
}
