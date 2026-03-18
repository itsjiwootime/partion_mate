package com.project.partition_mate.dto;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyCloseReason;
import com.project.partition_mate.domain.PartyStatus;
import com.project.partition_mate.domain.PackagingType;
import com.project.partition_mate.domain.StorageType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PartyResponse {

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
    private final String pickupPlace;
    private final LocalDateTime pickupTime;
    private final String pickupTimeLabel;
    private final LocalDateTime deadline;
    private final String deadlineLabel;
    private final LocalDateTime closedAt;
    private final PartyCloseReason closeReason;
    private final boolean favorite;

    public PartyResponse(Long id,
                         String title,
                         String productName,
                         Integer totalPrice,
                         Integer totalQuantity,
                         PartyStatus status,
                         String storeName,
                         Long currentQuantity,
                         String openChatUrl) {
        this(
                id,
                title,
                productName,
                totalPrice,
                totalPrice,
                null,
                totalQuantity,
                status,
                storeName,
                currentQuantity != null ? currentQuantity.intValue() : 0,
                openChatUrl,
                "개",
                1,
                StorageType.ROOM_TEMPERATURE,
                PackagingType.ORIGINAL_PACKAGE,
                false,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false
        );
    }

    public PartyResponse(Long id,
                         String title,
                         String productName,
                         Integer totalPrice,
                         Integer expectedTotalPrice,
                         Integer actualTotalPrice,
                         Integer totalQuantity,
                         PartyStatus status,
                         String storeName,
                         Long currentQuantity,
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
                         PartyCloseReason closeReason) {
        this(
                id,
                title,
                productName,
                totalPrice,
                expectedTotalPrice,
                actualTotalPrice,
                totalQuantity,
                status,
                storeName,
                currentQuantity != null ? currentQuantity.intValue() : 0,
                openChatUrl,
                unitLabel,
                minimumShareUnit,
                storageType,
                packagingType,
                hostProvidesPackaging,
                onSiteSplit,
                guideNote,
                receiptNote,
                pickupPlace,
                pickupTime,
                deadline,
                closedAt,
                closeReason,
                false
        );
    }

    public PartyResponse(Long id,
                         String title,
                         String productName,
                         Integer totalPrice,
                         Integer totalQuantity,
                         PartyStatus status,
                         String storeName,
                         Integer currentQuantity,
                         String openChatUrl) {
        this(
                id,
                title,
                productName,
                totalPrice,
                totalPrice,
                null,
                totalQuantity,
                status,
                storeName,
                currentQuantity,
                openChatUrl,
                "개",
                1,
                StorageType.ROOM_TEMPERATURE,
                PackagingType.ORIGINAL_PACKAGE,
                false,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false
        );
    }

    public PartyResponse(Long id,
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
                         String pickupPlace,
                         LocalDateTime pickupTime,
                         LocalDateTime deadline,
                         LocalDateTime closedAt,
                         PartyCloseReason closeReason) {
        this(
                id,
                title,
                productName,
                totalPrice,
                expectedTotalPrice,
                actualTotalPrice,
                totalQuantity,
                status,
                storeName,
                currentQuantity,
                openChatUrl,
                unitLabel,
                minimumShareUnit,
                storageType,
                packagingType,
                hostProvidesPackaging,
                onSiteSplit,
                guideNote,
                receiptNote,
                pickupPlace,
                pickupTime,
                deadline,
                closedAt,
                closeReason,
                false
        );
    }

    private PartyResponse(Long id,
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
                         String pickupPlace,
                         LocalDateTime pickupTime,
                         LocalDateTime deadline,
                         LocalDateTime closedAt,
                         PartyCloseReason closeReason,
                         boolean favorite) {
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
        this.pickupPlace = pickupPlace;
        this.pickupTime = pickupTime;
        this.pickupTimeLabel = DateTimeLabelFormatter.format(pickupTime);
        this.deadline = deadline;
        this.deadlineLabel = DateTimeLabelFormatter.format(deadline);
        this.closedAt = closedAt;
        this.closeReason = closeReason;
        this.favorite = favorite;
    }

    public static PartyResponse from(Party party) {
        return new PartyResponse(
                party.getId(),
                party.getTitle(),
                party.getProductName(),
                party.getDisplayTotalPrice(),
                party.getExpectedTotalPrice(),
                party.getActualTotalPrice(),
                party.getTotalQuantity(),
                party.getPartyStatus(),
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
                party.getCloseReason()
        );
    }

    public PartyResponse withFavorite(boolean favorite) {
        return new PartyResponse(
                this.id,
                this.title,
                this.productName,
                this.totalPrice,
                this.expectedTotalPrice,
                this.actualTotalPrice,
                this.totalQuantity,
                this.status,
                this.storeName,
                this.currentQuantity,
                this.openChatUrl,
                this.unitLabel,
                this.minimumShareUnit,
                this.storageType,
                this.packagingType,
                this.hostProvidesPackaging,
                this.onSiteSplit,
                this.guideNote,
                this.receiptNote,
                this.pickupPlace,
                this.pickupTime,
                this.deadline,
                this.closedAt,
                this.closeReason,
                favorite
        );
    }
}
