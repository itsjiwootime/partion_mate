package com.project.partition_mate.dto;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyCloseReason;
import com.project.partition_mate.domain.PartyStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class PartyRealtimeEventResponse {

    private final String eventId;
    private final Long id;
    private final String title;
    private final String productName;
    private final Integer totalPrice;
    private final Integer totalQuantity;
    private final PartyStatus status;
    private final Long storeId;
    private final String storeName;
    private final Integer currentQuantity;
    private final String openChatUrl;
    private final LocalDateTime deadline;
    private final String deadlineLabel;
    private final LocalDateTime closedAt;
    private final PartyCloseReason closeReason;
    private final Integer remainingQuantity;
    private final PartyRealtimeTrigger realtimeTrigger;
    private final LocalDateTime changedAt;

    private PartyRealtimeEventResponse(String eventId,
                                       Long id,
                                       String title,
                                       String productName,
                                       Integer totalPrice,
                                       Integer totalQuantity,
                                       PartyStatus status,
                                       Long storeId,
                                       String storeName,
                                       Integer currentQuantity,
                                       String openChatUrl,
                                       LocalDateTime deadline,
                                       LocalDateTime closedAt,
                                       PartyCloseReason closeReason,
                                       Integer remainingQuantity,
                                       PartyRealtimeTrigger realtimeTrigger,
                                       LocalDateTime changedAt) {
        this.eventId = eventId;
        this.id = id;
        this.title = title;
        this.productName = productName;
        this.totalPrice = totalPrice;
        this.totalQuantity = totalQuantity;
        this.status = status;
        this.storeId = storeId;
        this.storeName = storeName;
        this.currentQuantity = currentQuantity;
        this.openChatUrl = openChatUrl;
        this.deadline = deadline;
        this.deadlineLabel = DateTimeLabelFormatter.format(deadline);
        this.closedAt = closedAt;
        this.closeReason = closeReason;
        this.remainingQuantity = remainingQuantity;
        this.realtimeTrigger = realtimeTrigger;
        this.changedAt = changedAt;
    }

    public static PartyRealtimeEventResponse from(Party party,
                                                  PartyRealtimeTrigger realtimeTrigger,
                                                  LocalDateTime changedAt) {
        return new PartyRealtimeEventResponse(
                UUID.randomUUID().toString(),
                party.getId(),
                party.getTitle(),
                party.getProductName(),
                party.getTotalPrice(),
                party.getTotalQuantity(),
                party.getPartyStatus(),
                party.getStore() != null ? party.getStore().getId() : null,
                party.getStore() != null ? party.getStore().getName() : null,
                party.getRequestedQuantity(),
                party.getOpenChatUrl(),
                party.getDeadline(),
                party.getClosedAt(),
                party.getCloseReason(),
                party.getRemainingQuantity(),
                realtimeTrigger,
                changedAt
        );
    }
}
