package com.project.partition_mate.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateUserBlockRequest {

    @NotNull(message = "차단할 사용자 ID를 입력해 주세요.")
    private Long targetUserId;
}
