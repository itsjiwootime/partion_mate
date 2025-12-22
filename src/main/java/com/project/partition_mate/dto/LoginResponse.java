package com.project.partition_mate.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
public class LoginResponse {
    private String accessToken;

    public LoginResponse(String accessToken) {
        this.accessToken = accessToken;
    }
}
