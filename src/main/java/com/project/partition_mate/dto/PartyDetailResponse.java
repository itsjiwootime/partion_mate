package com.project.partition_mate.dto;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyCloseReason;
import com.project.partition_mate.domain.PartyStatus;
import com.project.partition_mate.domain.PackagingType;
import com.project.partition_mate.domain.StorageType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PartyDetailResponse {

    private final Long id;
    private final String title;
    private final String productName;
    private final Integer totalPrice;
    private final Integer expectedTotalPrice;
    private final Integer actualTotalPrice;
    private final Integer totalQuantity;
    private final PartyStatus status;
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
    private final LocalDateTime deadline;
    private final String deadlineLabel;
    private final LocalDateTime closedAt;
    private final PartyCloseReason closeReason;

    private PartyDetailResponse(Long id,
                                String title,
                                String productName,
                                Integer totalPrice,
                                Integer expectedTotalPrice,
                                Integer actualTotalPrice,
                                Integer totalQuantity,
                                PartyStatus status,
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
                                LocalDateTime deadline,
                                LocalDateTime closedAt,
                                PartyCloseReason closeReason) {
        this.id = id;
        this.title = title;
        this.productName = productName;
        this.totalPrice = totalPrice;
        this.expectedTotalPrice = expectedTotalPrice;
        this.actualTotalPrice = actualTotalPrice;
        this.totalQuantity = totalQuantity;
        this.status = status;
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
        this.deadline = deadline;
        this.deadlineLabel = DateTimeLabelFormatter.format(deadline);
        this.closedAt = closedAt;
        this.closeReason = closeReason;
    }

    public static PartyDetailResponse from(Party party) {
        return new PartyDetailResponse(
                party.getId(),
                party.getTitle(),
                party.getProductName(),
                party.getDisplayTotalPrice(),
                party.getExpectedTotalPrice(),
                party.getActualTotalPrice(),
                party.getTotalQuantity(),
                party.getPartyStatus(),
                party.getStore().getName(),
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
                party.getDeadline(),
                party.getClosedAt(),
                party.getCloseReason()
        );
    }
}
