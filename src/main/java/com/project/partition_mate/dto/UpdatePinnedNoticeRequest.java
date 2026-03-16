package com.project.partition_mate.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UpdatePinnedNoticeRequest {

    @Size(max = 500, message = "호스트 공지는 500자를 초과할 수 없습니다.")
    private String pinnedNotice;
}
