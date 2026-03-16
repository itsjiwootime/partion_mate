package com.project.partition_mate.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ConfirmPickupScheduleRequest {

    @NotBlank(message = "픽업 장소는 필수입니다.")
    private String pickupPlace;

    @NotNull(message = "픽업 시간은 필수입니다.")
    @Future(message = "픽업 시간은 현재보다 미래여야 합니다.")
    private LocalDateTime pickupTime;
}
