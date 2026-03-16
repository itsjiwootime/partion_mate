package com.project.partition_mate.dto;

import com.project.partition_mate.domain.ParticipationStatus;
import com.project.partition_mate.domain.PartyCloseReason;
import com.project.partition_mate.domain.PartyStatus;
import com.project.partition_mate.domain.PartyMemberRole;
import lombok.Getter;

import java.time.LocalDateTime;

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
    private final LocalDateTime deadline;
    private final String deadlineLabel;
    private final LocalDateTime closedAt;
    private final PartyCloseReason closeReason;

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
                                  Integer requestedQuantity,
                                  LocalDateTime deadline,
                                  LocalDateTime closedAt,
                                  PartyCloseReason closeReason) {
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
        this.deadline = deadline;
        this.deadlineLabel = DateTimeLabelFormatter.format(deadline);
        this.closedAt = closedAt;
        this.closeReason = closeReason;
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
                                               Integer requestedQuantity,
                                               LocalDateTime deadline,
                                               LocalDateTime closedAt,
                                               PartyCloseReason closeReason) {
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
                requestedQuantity,
                deadline,
                closedAt,
                closeReason
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
                                                Integer requestedQuantity,
                                                LocalDateTime deadline,
                                                LocalDateTime closedAt,
                                                PartyCloseReason closeReason) {
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
                requestedQuantity,
                deadline,
                closedAt,
                closeReason
        );
    }
}
