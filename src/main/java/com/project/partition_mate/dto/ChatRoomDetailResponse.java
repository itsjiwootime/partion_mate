package com.project.partition_mate.dto;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class ChatRoomDetailResponse {

    private final Long roomId;
    private final Long partyId;
    private final String partyTitle;
    private final String storeName;
    private final boolean host;
    private final String pinnedNotice;
    private final LocalDateTime pinnedNoticeUpdatedAt;
    private final String pinnedNoticeUpdatedAtLabel;
    private final long unreadCount;
    private final List<ChatMessageResponse> messages;

    public ChatRoomDetailResponse(Long roomId,
                                  Long partyId,
                                  String partyTitle,
                                  String storeName,
                                  boolean host,
                                  String pinnedNotice,
                                  LocalDateTime pinnedNoticeUpdatedAt,
                                  long unreadCount,
                                  List<ChatMessageResponse> messages) {
        this.roomId = roomId;
        this.partyId = partyId;
        this.partyTitle = partyTitle;
        this.storeName = storeName;
        this.host = host;
        this.pinnedNotice = pinnedNotice;
        this.pinnedNoticeUpdatedAt = pinnedNoticeUpdatedAt;
        this.pinnedNoticeUpdatedAtLabel = DateTimeLabelFormatter.format(pinnedNoticeUpdatedAt);
        this.unreadCount = unreadCount;
        this.messages = messages;
    }
}
