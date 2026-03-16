package com.project.partition_mate.dto;

import com.project.partition_mate.domain.ChatMessage;
import com.project.partition_mate.domain.ChatMessageType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChatMessageResponse {

    private final Long messageId;
    private final Long partyId;
    private final ChatMessageType type;
    private final Long senderId;
    private final String senderName;
    private final String content;
    private final boolean mine;
    private final LocalDateTime createdAt;
    private final String createdAtLabel;
    private final String pinnedNotice;

    private ChatMessageResponse(Long messageId,
                                Long partyId,
                                ChatMessageType type,
                                Long senderId,
                                String senderName,
                                String content,
                                boolean mine,
                                LocalDateTime createdAt,
                                String pinnedNotice) {
        this.messageId = messageId;
        this.partyId = partyId;
        this.type = type;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.mine = mine;
        this.createdAt = createdAt;
        this.createdAtLabel = DateTimeLabelFormatter.format(createdAt);
        this.pinnedNotice = pinnedNotice;
    }

    public static ChatMessageResponse from(ChatMessage message, Long currentUserId) {
        Long senderId = message.getSender() != null ? message.getSender().getId() : null;
        return new ChatMessageResponse(
                message.getId(),
                message.getChatRoom().getParty().getId(),
                message.getType(),
                senderId,
                message.getSenderName(),
                message.getContent(),
                senderId != null && senderId.equals(currentUserId),
                message.getCreatedAt(),
                null
        );
    }

    public static ChatMessageResponse from(ChatMessage message, Long currentUserId, String pinnedNotice) {
        Long senderId = message.getSender() != null ? message.getSender().getId() : null;
        return new ChatMessageResponse(
                message.getId(),
                message.getChatRoom().getParty().getId(),
                message.getType(),
                senderId,
                message.getSenderName(),
                message.getContent(),
                senderId != null && senderId.equals(currentUserId),
                message.getCreatedAt(),
                pinnedNotice
        );
    }
}
