package com.project.partition_mate.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "chat_message",
        indexes = @Index(name = "idx_chat_message_room_id_id", columnList = "chat_room_id, id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Column(nullable = false)
    private String senderName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatMessageType type;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private ChatMessage(ChatRoom chatRoom,
                        User sender,
                        String senderName,
                        ChatMessageType type,
                        String content,
                        LocalDateTime createdAt) {
        this.chatRoom = Objects.requireNonNull(chatRoom, "채팅방은 필수입니다.");
        this.sender = sender;
        this.senderName = Objects.requireNonNull(senderName, "작성자 이름은 필수입니다.");
        this.type = Objects.requireNonNull(type, "메시지 타입은 필수입니다.");
        this.content = validateContent(content);
        this.createdAt = Objects.requireNonNull(createdAt, "생성 시각은 필수입니다.");
    }

    public static ChatMessage text(ChatRoom chatRoom, User sender, String content, LocalDateTime createdAt) {
        return new ChatMessage(
                chatRoom,
                Objects.requireNonNull(sender, "작성자는 필수입니다."),
                sender.getUsername(),
                ChatMessageType.TEXT,
                content,
                createdAt
        );
    }

    public static ChatMessage system(ChatRoom chatRoom, String content, LocalDateTime createdAt) {
        return new ChatMessage(chatRoom, null, "시스템", ChatMessageType.SYSTEM, content, createdAt);
    }

    private String validateContent(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("메시지 내용은 비어 있을 수 없습니다.");
        }
        if (normalized.length() > 1000) {
            throw new IllegalArgumentException("메시지는 1000자를 초과할 수 없습니다.");
        }
        return normalized;
    }
}
