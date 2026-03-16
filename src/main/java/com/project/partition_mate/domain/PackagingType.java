package com.project.partition_mate.domain;

import lombok.Getter;

@Getter
public enum PackagingType {
    ORIGINAL_PACKAGE("원포장"),
    ZIP_BAG("지퍼백"),
    CONTAINER("용기"),
    OTHER("기타");

    private final String label;

    PackagingType(String label) {
        this.label = label;
    }
}
