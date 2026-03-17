package com.project.partition_mate.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
public class LoginResponse {
    private final String accessToken;
    private final long accessTokenExpiresInMs;

    public LoginResponse(String accessToken, long accessTokenExpiresInMs) {
        this.accessToken = accessToken;
        this.accessTokenExpiresInMs = accessTokenExpiresInMs;
    }
}
