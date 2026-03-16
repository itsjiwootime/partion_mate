package com.project.partition_mate.dto;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyCloseReason;
import com.project.partition_mate.domain.PartyStatus;
import com.project.partition_mate.domain.PackagingType;
import com.project.partition_mate.domain.StorageType;
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
    private final Integer expectedTotalPrice;
    private final Integer actualTotalPrice;
    private final Integer totalQuantity;
    private final PartyStatus status;
    private final Long storeId;
    private final String storeName;
    private final Integer currentQuantity;
    private final String openChatUrl;
    private final String unitLabel;
    private final Integer minimumShareUnit;
    private final StorageType storageType;
    private final String storageTypeLabel;
    private final PackagingType packagingType;
    private final String packagingTypeLabel;
    private final boolean hostProvidesPackaging;
    private final boolean onSiteSplit;
    private final String guideNote;
    private final String receiptNote;
    private final String pickupPlace;
    private final LocalDateTime pickupTime;
    private final String pickupTimeLabel;
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
                                       Integer expectedTotalPrice,
                                       Integer actualTotalPrice,
                                       Integer totalQuantity,
                                       PartyStatus status,
                                       Long storeId,
                                       String storeName,
                                       Integer currentQuantity,
                                       String openChatUrl,
                                       String unitLabel,
                                       Integer minimumShareUnit,
                                       StorageType storageType,
                                       PackagingType packagingType,
                                       boolean hostProvidesPackaging,
                                       boolean onSiteSplit,
                                       String guideNote,
                                       String receiptNote,
                                       String pickupPlace,
                                       LocalDateTime pickupTime,
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
        this.expectedTotalPrice = expectedTotalPrice;
        this.actualTotalPrice = actualTotalPrice;
        this.totalQuantity = totalQuantity;
        this.status = status;
        this.storeId = storeId;
        this.storeName = storeName;
        this.currentQuantity = currentQuantity;
        this.openChatUrl = openChatUrl;
        this.unitLabel = unitLabel;
        this.minimumShareUnit = minimumShareUnit;
        this.storageType = storageType;
        this.storageTypeLabel = storageType != null ? storageType.getLabel() : null;
        this.packagingType = packagingType;
        this.packagingTypeLabel = packagingType != null ? packagingType.getLabel() : null;
        this.hostProvidesPackaging = hostProvidesPackaging;
        this.onSiteSplit = onSiteSplit;
        this.guideNote = guideNote;
        this.receiptNote = receiptNote;
        this.pickupPlace = pickupPlace;
        this.pickupTime = pickupTime;
        this.pickupTimeLabel = DateTimeLabelFormatter.format(pickupTime);
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
                party.getDisplayTotalPrice(),
                party.getExpectedTotalPrice(),
                party.getActualTotalPrice(),
                party.getTotalQuantity(),
                party.getPartyStatus(),
                party.getStore() != null ? party.getStore().getId() : null,
                party.getStore() != null ? party.getStore().getName() : null,
                party.getRequestedQuantity(),
                party.getOpenChatUrl(),
                party.getUnitLabel(),
                party.getMinimumShareUnit(),
                party.getStorageType(),
                party.getPackagingType(),
                party.isHostProvidesPackaging(),
                party.isOnSiteSplit(),
                party.getGuideNote(),
                party.getReceiptNote(),
                party.getPickupPlace(),
                party.getPickupTime(),
                party.getDeadline(),
                party.getClosedAt(),
                party.getCloseReason(),
                party.getRemainingQuantity(),
                realtimeTrigger,
                changedAt
        );
    }
}
