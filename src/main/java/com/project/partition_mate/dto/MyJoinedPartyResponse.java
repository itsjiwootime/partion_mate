package com.project.partition_mate.dto;

import com.project.partition_mate.domain.PackagingType;
import com.project.partition_mate.domain.ParticipationStatus;
import com.project.partition_mate.domain.PartyCloseReason;
import com.project.partition_mate.domain.PartyMemberRole;
import com.project.partition_mate.domain.PartyStatus;
import com.project.partition_mate.domain.StorageType;
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
    private final Integer expectedTotalPrice;
    private final Integer actualTotalPrice;
    private final String openChatUrl;
    private final ParticipationStatus participationStatus;
    private final Integer waitingPosition;
    private final Integer requestedQuantity;
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

    private MyJoinedPartyResponse(Long id,
                                  String title,
                                  String productName,
                                  String storeName,
                                  PartyStatus status,
                                  Integer totalQuantity,
                                  Integer currentQuantity,
                                  PartyMemberRole userRole,
                                  Integer totalPrice,
                                  Integer expectedTotalPrice,
                                  Integer actualTotalPrice,
                                  String openChatUrl,
                                  ParticipationStatus participationStatus,
                                  Integer waitingPosition,
                                  Integer requestedQuantity,
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
        this.storeName = storeName;
        this.status = status;
        this.totalQuantity = totalQuantity;
        this.currentQuantity = currentQuantity;
        this.userRole = userRole;
        this.totalPrice = totalPrice;
        this.expectedTotalPrice = expectedTotalPrice;
        this.actualTotalPrice = actualTotalPrice;
        this.openChatUrl = openChatUrl;
        this.participationStatus = participationStatus;
        this.waitingPosition = waitingPosition;
        this.requestedQuantity = requestedQuantity;
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

    public static MyJoinedPartyResponse joined(Long id,
                                               String title,
                                               String productName,
                                               String storeName,
                                               PartyStatus status,
                                               Integer totalQuantity,
                                               Integer currentQuantity,
                                               PartyMemberRole userRole,
                                               Integer totalPrice,
                                               Integer expectedTotalPrice,
                                               Integer actualTotalPrice,
                                               String openChatUrl,
                                               Integer requestedQuantity,
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
                expectedTotalPrice,
                actualTotalPrice,
                openChatUrl,
                ParticipationStatus.JOINED,
                null,
                requestedQuantity,
                unitLabel,
                minimumShareUnit,
                storageType,
                packagingType,
                hostProvidesPackaging,
                onSiteSplit,
                guideNote,
                receiptNote,
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
                                                Integer expectedTotalPrice,
                                                Integer actualTotalPrice,
                                                String openChatUrl,
                                                Integer waitingPosition,
                                                Integer requestedQuantity,
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
                expectedTotalPrice,
                actualTotalPrice,
                openChatUrl,
                ParticipationStatus.WAITING,
                waitingPosition,
                requestedQuantity,
                unitLabel,
                minimumShareUnit,
                storageType,
                packagingType,
                hostProvidesPackaging,
                onSiteSplit,
                guideNote,
                receiptNote,
                deadline,
                closedAt,
                closeReason
        );
    }
}
