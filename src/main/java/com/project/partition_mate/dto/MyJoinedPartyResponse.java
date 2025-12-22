package com.project.partition_mate.dto;

import com.project.partition_mate.domain.PartyStatus;
import com.project.partition_mate.domain.PartyMemberRole;
import lombok.Getter;

@Getter
public class MyJoinedPartyResponse {

    private final Long id;
    private final String title;
    private final String productName;
    private final String storeName;
    private final PartyStatus status;
    private final Integer totalQuantity;
    private final Integer currentQuantity;
    private final PartyMemberRole userRole;
    private final Integer totalPrice;
    private final String openChatUrl;

    private MyJoinedPartyResponse(Long id,
                                  String title,
                                  String productName,
                                  String storeName,
                                  PartyStatus status,
                                  Integer totalQuantity,
                                  Integer currentQuantity,
                                  PartyMemberRole userRole,
                                  Integer totalPrice,
                                  String openChatUrl) {
        this.id = id;
        this.title = title;
        this.productName = productName;
        this.storeName = storeName;
        this.status = status;
        this.totalQuantity = totalQuantity;
        this.currentQuantity = currentQuantity;
        this.userRole = userRole;
        this.totalPrice = totalPrice;
        this.openChatUrl = openChatUrl;
    }

    public static MyJoinedPartyResponse of(Long id,
                                           String title,
                                           String productName,
                                           String storeName,
                                           PartyStatus status,
                                           Integer totalQuantity,
                                           Integer currentQuantity,
                                           PartyMemberRole userRole,
                                           Integer totalPrice,
                                           String openChatUrl) {
        return new MyJoinedPartyResponse(
                id,
                title,
                productName,
                storeName,
                status,
                totalQuantity,
                currentQuantity,
                userRole,
                totalPrice,
                openChatUrl
        );
    }
}
