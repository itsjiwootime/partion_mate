package com.project.partition_mate.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChatRoomSummaryResponse {

    private final Long roomId;
    private final Long partyId;
    private final String partyTitle;
    private final String storeName;
    private final String pinnedNotice;
    private final String lastMessagePreview;
    private final String lastMessageType;
    private final LocalDateTime lastMessageCreatedAt;
    private final String lastMessageCreatedAtLabel;
    private final long unreadCount;

    public ChatRoomSummaryResponse(Long roomId,
                                   Long partyId,
                                   String partyTitle,
                                   String storeName,
                                   String pinnedNotice,
                                   String lastMessagePreview,
                                   String lastMessageType,
                                   LocalDateTime lastMessageCreatedAt,
                                   long unreadCount) {
        this.roomId = roomId;
        this.partyId = partyId;
        this.partyTitle = partyTitle;
        this.storeName = storeName;
        this.pinnedNotice = pinnedNotice;
        this.lastMessagePreview = lastMessagePreview;
        this.lastMessageType = lastMessageType;
        this.lastMessageCreatedAt = lastMessageCreatedAt;
        this.lastMessageCreatedAtLabel = DateTimeLabelFormatter.format(lastMessageCreatedAt);
        this.unreadCount = unreadCount;
    }
}
