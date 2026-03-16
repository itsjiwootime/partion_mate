package com.project.partition_mate.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
        name = "store",
        indexes = {
                @Index(name = "idx_store_latitude_longitude", columnList = "latitude, longitude")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private LocalTime openTime;

    @Column(nullable = false)
    private LocalTime closeTime;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @OneToMany(mappedBy = "store" , cascade = CascadeType.ALL)
    private List<Party> partyList = new ArrayList<>();

    public Store(String name,
                 String address,
                 LocalTime openTime,
                 LocalTime closeTime,
                 double latitude,
                 double longitude,
                 String phone) {

        this.name = Objects.requireNonNull(name);
        this.address = Objects.requireNonNull(address);
        this.openTime = Objects.requireNonNull(openTime);
        this.closeTime = Objects.requireNonNull(closeTime);
        this.longitude = longitude;
        this.latitude = latitude;
        this.phone = phone;

        validateBusinessHour(openTime, closeTime);
        validateCoordinate(latitude, longitude);

    }

    private void validateBusinessHour(LocalTime open, LocalTime close) {
        if (!open.isBefore(close)) {
            throw new IllegalArgumentException("영업 시작 시간은 종료 시간보다 빨라야 합니다.");
        }
    }

    private void validateCoordinate(Double lat, Double lon) {
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            throw new IllegalArgumentException("유효하지 않은 위경도 범위입니다.");
        }
    }





}
