package com.project.partition_mate.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "chat_read_state",
        uniqueConstraints = @UniqueConstraint(columnNames = {"chat_room_id", "user_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatReadState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    private LocalDateTime lastReadAt;

    private ChatReadState(ChatRoom chatRoom, User user) {
        this.chatRoom = Objects.requireNonNull(chatRoom, "채팅방은 필수입니다.");
        this.user = Objects.requireNonNull(user, "사용자는 필수입니다.");
    }

    public static ChatReadState create(ChatRoom chatRoom, User user) {
        return new ChatReadState(chatRoom, user);
    }

    public void markRead(Long messageId, LocalDateTime readAt) {
        this.lastReadMessageId = messageId;
        this.lastReadAt = Objects.requireNonNull(readAt, "읽은 시각은 필수입니다.");
    }
}
