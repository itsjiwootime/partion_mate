package com.project.partition_mate.dto;

import com.project.partition_mate.domain.User;
import lombok.Getter;

@Getter
public class SignUpResponse {

    private String username;

    private SignUpResponse(String username) {
        this.username =  username;
    }

    public static SignUpResponse from(User user) {
        return new SignUpResponse(user.getUsername());
    }
}
