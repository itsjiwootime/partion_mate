package com.project.partition_mate.dto;

import com.project.partition_mate.domain.User;
import com.project.partition_mate.service.UserService;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
public class UserResponse {

    private String name;
    private String email;
    private String address;
    private Double latitude;
    private Double longitude;

    private UserResponse(String name, String email, String address, Double latitude, Double longitude) {
        this.name = name;
        this.email = email;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getUsername(),
                user.getEmail(),
                user.getAddress(),
                user.getLatitude(),
                user.getLongitude()
        );
    }

}
