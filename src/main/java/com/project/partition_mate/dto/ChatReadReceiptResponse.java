package com.project.partition_mate.dto;

import lombok.Getter;

@Getter
public class ChatReadReceiptResponse {

    private final Long partyId;
    private final long unreadCount;

    public ChatReadReceiptResponse(Long partyId, long unreadCount) {
        this.partyId = partyId;
        this.unreadCount = unreadCount;
    }
}
