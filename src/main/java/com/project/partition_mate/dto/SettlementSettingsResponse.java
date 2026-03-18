package com.project.partition_mate.dto;

import com.project.partition_mate.domain.User;
import lombok.Getter;

@Getter
public class SettlementSettingsResponse {

    private final String settlementGuide;

    private SettlementSettingsResponse(String settlementGuide) {
        this.settlementGuide = settlementGuide;
    }

    public static SettlementSettingsResponse from(User user) {
        return new SettlementSettingsResponse(user.getSettlementGuide());
    }
}
