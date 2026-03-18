package com.project.partition_mate.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String address;


    private Double latitude;

    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;


    public User(String username, String email, String password, String address, Double latitude, Double longitude) {
        validateUsername(username);
        validateAddress(address);
        validateCoordinate(latitude, longitude);
        this.username = username.trim();
        this.email = Objects.requireNonNull(email);
        this.password = Objects.requireNonNull(password);
        this.address = address.trim();
        this.role = UserRole.USER;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void updateProfile(String username, String address) {
        validateUsername(username);
        validateAddress(address);
        this.username = username.trim();
        this.address = address.trim();
    }

    public void updateCoordinate(Double latitude, Double longitude) {
        validateCoordinate(latitude, longitude);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void clearCoordinate() {
        this.latitude = null;
        this.longitude = null;
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("닉네임은 비어 있을 수 없습니다.");
        }
    }

    private void validateAddress(String address) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("주소는 비어 있을 수 없습니다.");
        }
    }

    private void validateCoordinate(Double latitude, Double longitude) {
        if (latitude == null && longitude == null) {
            return;
        }

        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("위치 좌표는 위도와 경도를 함께 입력해야 합니다.");
        }

        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("위도는 -90 이상 90 이하여야 합니다.");
        }

        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("경도는 -180 이상 180 이하여야 합니다.");
        }
    }
}
