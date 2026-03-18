package com.project.partition_mate.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class UpsertWebPushSubscriptionRequest {

    @NotBlank
    private String endpoint;

    @Valid
    @NotNull
    private Keys keys;

    private String userAgent;

    @Getter
    public static class Keys {

        @NotBlank
        private String p256dh;

        @NotBlank
        private String auth;
    }
}
