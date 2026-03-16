package com.project.partition_mate.dto;

import com.project.partition_mate.domain.User;
import lombok.Getter;

import java.util.List;

@Getter
public class UserResponse {

    private String name;
    private String email;
    private String address;
    private Double latitude;
    private Double longitude;
    private TrustSummaryResponse trustSummary;
    private List<ReviewResponse> recentReviews;

    private UserResponse(String name,
                         String email,
                         String address,
                         Double latitude,
                         Double longitude,
                         TrustSummaryResponse trustSummary,
                         List<ReviewResponse> recentReviews) {
        this.name = name;
        this.email = email;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.trustSummary = trustSummary;
        this.recentReviews = recentReviews;
    }

    public static UserResponse from(User user,
                                    TrustSummaryResponse trustSummary,
                                    List<ReviewResponse> recentReviews) {
        return new UserResponse(
                user.getUsername(),
                user.getEmail(),
                user.getAddress(),
                user.getLatitude(),
                user.getLongitude(),
                trustSummary,
                recentReviews
        );
    }

}
