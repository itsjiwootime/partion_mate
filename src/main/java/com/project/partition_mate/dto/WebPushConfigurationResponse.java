package com.project.partition_mate.dto;

import lombok.Getter;

@Getter
public class WebPushConfigurationResponse {

    private final boolean enabled;
    private final String publicKey;

    private WebPushConfigurationResponse(boolean enabled, String publicKey) {
        this.enabled = enabled;
        this.publicKey = publicKey;
    }

    public static WebPushConfigurationResponse of(boolean enabled, String publicKey) {
        return new WebPushConfigurationResponse(enabled, publicKey);
    }
}
