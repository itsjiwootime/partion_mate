package com.project.partition_mate.dto;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyCloseReason;
import com.project.partition_mate.domain.PartyStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PartyResponse {

    private final Long id;
    private final String title;
    private final String productName;
    private final Integer totalPrice;
    private final Integer totalQuantity;
    private final PartyStatus status;
    private final String storeName;
    private final Integer currentQuantity;
    private final String openChatUrl;
    private final LocalDateTime deadline;
    private final String deadlineLabel;
    private final LocalDateTime closedAt;
    private final PartyCloseReason closeReason;

    public PartyResponse(Long id, String title, String productName, Integer totalPrice, Integer totalQuantity, PartyStatus status, String storeName, Long currentQuantity, String openChatUrl) {
        this(
                id,
                title,
                productName,
                totalPrice,
                totalQuantity,
                status,
                storeName,
                currentQuantity != null ? currentQuantity.intValue() : 0,
                openChatUrl,
                null,
                null,
                null
        );
    }

    public PartyResponse(Long id,
                         String title,
                         String productName,
                         Integer totalPrice,
                         Integer totalQuantity,
                         PartyStatus status,
                         String storeName,
                         Long currentQuantity,
                         String openChatUrl,
                         LocalDateTime deadline,
                         LocalDateTime closedAt,
                         PartyCloseReason closeReason) {
        this(
                id,
                title,
                productName,
                totalPrice,
                totalQuantity,
                status,
                storeName,
                currentQuantity != null ? currentQuantity.intValue() : 0,
                openChatUrl,
                deadline,
                closedAt,
                closeReason
        );
    }

    public PartyResponse(Long id, String title, String productName, Integer totalPrice, Integer totalQuantity, PartyStatus status, String storeName, Integer currentQuantity, String openChatUrl) {
        this(id, title, productName, totalPrice, totalQuantity, status, storeName, currentQuantity, openChatUrl, null, null, null);
    }

    public PartyResponse(Long id,
                         String title,
                         String productName,
                         Integer totalPrice,
                         Integer totalQuantity,
                         PartyStatus status,
                         String storeName,
                         Integer currentQuantity,
                         String openChatUrl,
                         LocalDateTime deadline,
                         LocalDateTime closedAt,
                         PartyCloseReason closeReason) {
        this.id = id;
        this.title = title;
        this.productName = productName;
        this.totalPrice = totalPrice;
        this.totalQuantity = totalQuantity;
        this.status = status;
        this.storeName = storeName;
        this.currentQuantity = currentQuantity;
        this.openChatUrl = openChatUrl;
        this.deadline = deadline;
        this.deadlineLabel = DateTimeLabelFormatter.format(deadline);
        this.closedAt = closedAt;
        this.closeReason = closeReason;
    }

    public static PartyResponse from(Party party) {
        return new PartyResponse(
                party.getId(),
                party.getTitle(),
                party.getProductName(),
                party.getTotalPrice(),
                party.getTotalQuantity(),
                party.getPartyStatus(),
                party.getStore() != null ? party.getStore().getName() : null,
                party.getRequestedQuantity(),
                party.getOpenChatUrl(),
                party.getDeadline(),
                party.getClosedAt(),
                party.getCloseReason()
        );
    }


}
