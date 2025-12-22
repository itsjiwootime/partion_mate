package com.project.partition_mate.dto;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyStatus;
import lombok.Builder;
import lombok.Getter;

@Getter

public class PartyDetailResponse {

    private final Long id;
    private final String title;
    private final String productName;
    private final Integer totalPrice;
    private final Integer totalQuantity;
    private final PartyStatus status;
    private final String storeName;
    private final Integer currentQuantity;
    private final String openChatUrl;


    private PartyDetailResponse(Long id, String title, String productName, Integer totalPrice, Integer totalQuantity, PartyStatus status, String storeName, Integer currentQuantity, String openChatUrl) {
        this.id = id;
        this.title = title;
        this.productName = productName;
        this.totalPrice = totalPrice;
        this.totalQuantity = totalQuantity;
        this.status = status;
        this.storeName = storeName;
        this.currentQuantity = currentQuantity;
        this.openChatUrl = openChatUrl;
    }

    public static PartyDetailResponse from(Party party) {
        return new PartyDetailResponse(
                party.getId(),
                party.getTitle(),
                party.getProductName(),
                party.getTotalPrice(),
                party.getTotalQuantity(),
                party.getPartyStatus(),
                party.getStore().getName(),
                party.getRequestedQuantity(),
                party.getOpenChatUrl()
        );
    }
}
