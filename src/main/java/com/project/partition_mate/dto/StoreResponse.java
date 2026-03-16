package com.project.partition_mate.dto;

import com.project.partition_mate.domain.Store;
import lombok.Getter;

import java.time.LocalTime;

@Getter
public class StoreResponse {

    private final Long id;
    private final String name;
    private final String address;
    private final String openTime;
    private final String closeTime;
    private final String phone;
    private final Double distance;
    private final Long partyCount;

    public StoreResponse(Long id,
                         String name,
                         String address,
                         LocalTime openTime,
                         LocalTime closeTime,
                         String phone,
                         Double distance,
                         Long partyCount) {
        this(
                id,
                name,
                address,
                openTime != null ? openTime.toString() : null,
                closeTime != null ? closeTime.toString() : null,
                phone,
                distance,
                partyCount
        );
    }

    private StoreResponse(Long id,
                          String name,
                          String address,
                          String openTime,
                          String closeTime,
                          String phone,
                          Double distance,
                          Long partyCount) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.phone = phone;
        this.distance = distance;
        this.partyCount = partyCount;
    }

    public static StoreResponse from(Store store, Long partyCount) {
        return new StoreResponse(
                store.getId(),
                store.getName(),
                store.getAddress(),
                store.getOpenTime().toString(),
                store.getCloseTime().toString(),
                store.getPhone(),
                null,
                partyCount
        );
    }

    public static StoreResponse of(Store store, Double distance, Long partyCount) {
        return new StoreResponse(
                store.getId(),
                store.getName(),
                store.getAddress(),
                store.getOpenTime().toString(),
                store.getCloseTime().toString(),
                store.getPhone(),
                distance,
                partyCount
        );
    }
}
