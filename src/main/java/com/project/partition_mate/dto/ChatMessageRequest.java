package com.project.partition_mate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ChatMessageRequest {

    @NotBlank(message = "메시지 내용은 비어 있을 수 없습니다.")
    @Size(max = 1000, message = "메시지는 1000자를 초과할 수 없습니다.")
    private String content;
}
