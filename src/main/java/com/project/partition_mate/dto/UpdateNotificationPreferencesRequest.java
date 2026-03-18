package com.project.partition_mate.dto;

import com.project.partition_mate.domain.UserNotificationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

@Getter
public class UpdateNotificationPreferencesRequest {

    @Valid
    @NotEmpty(message = "최소 한 개 이상의 알림 설정이 필요합니다.")
    private List<Preference> preferences;

    @Getter
    public static class Preference {

        @NotNull(message = "알림 타입은 필수입니다.")
        private UserNotificationType type;

        private boolean webPushEnabled;
    }
}
