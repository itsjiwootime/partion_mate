package com.project.partition_mate.dto;

import com.project.partition_mate.domain.ParticipationStatus;
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
    private final ParticipationStatus participationStatus;
    private final Integer waitingPosition;
    private final Integer requestedQuantity;

    private MyJoinedPartyResponse(Long id,
                                  String title,
                                  String productName,
                                  String storeName,
                                  PartyStatus status,
                                  Integer totalQuantity,
                                  Integer currentQuantity,
                                  PartyMemberRole userRole,
                                  Integer totalPrice,
                                  String openChatUrl,
                                  ParticipationStatus participationStatus,
                                  Integer waitingPosition,
                                  Integer requestedQuantity) {
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
        this.participationStatus = participationStatus;
        this.waitingPosition = waitingPosition;
        this.requestedQuantity = requestedQuantity;
    }

    public static MyJoinedPartyResponse joined(Long id,
                                               String title,
                                               String productName,
                                               String storeName,
                                               PartyStatus status,
                                               Integer totalQuantity,
                                               Integer currentQuantity,
                                               PartyMemberRole userRole,
                                               Integer totalPrice,
                                               String openChatUrl,
                                               Integer requestedQuantity) {
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
                openChatUrl,
                ParticipationStatus.JOINED,
                null,
                requestedQuantity
        );
    }

    public static MyJoinedPartyResponse waiting(Long id,
                                                String title,
                                                String productName,
                                                String storeName,
                                                PartyStatus status,
                                                Integer totalQuantity,
                                                Integer currentQuantity,
                                                Integer totalPrice,
                                                String openChatUrl,
                                                Integer waitingPosition,
                                                Integer requestedQuantity) {
        return new MyJoinedPartyResponse(
                id,
                title,
                productName,
                storeName,
                status,
                totalQuantity,
                currentQuantity,
                null,
                totalPrice,
                openChatUrl,
                ParticipationStatus.WAITING,
                waitingPosition,
                requestedQuantity
        );
    }
}
