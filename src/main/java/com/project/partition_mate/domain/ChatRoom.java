package com.project.partition_mate.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "chat_room")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false, unique = true)
    private Party party;

    @Column(length = 500)
    private String pinnedNotice;

    private LocalDateTime pinnedNoticeUpdatedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> messages = new ArrayList<>();

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatReadState> readStates = new ArrayList<>();

    private ChatRoom(Party party, LocalDateTime createdAt) {
        this.party = Objects.requireNonNull(party, "파티는 필수입니다.");
        this.createdAt = Objects.requireNonNull(createdAt, "생성 시각은 필수입니다.");
        party.attachChatRoom(this);
    }

    public static ChatRoom create(Party party, LocalDateTime createdAt) {
        return new ChatRoom(party, createdAt);
    }

    public boolean updatePinnedNotice(String pinnedNotice, LocalDateTime updatedAt) {
        String normalized = normalizeNotice(pinnedNotice);
        if (Objects.equals(this.pinnedNotice, normalized)) {
            return false;
        }

        this.pinnedNotice = normalized;
        this.pinnedNoticeUpdatedAt = normalized == null ? null : Objects.requireNonNull(updatedAt, "공지 수정 시각은 필수입니다.");
        return true;
    }

    private String normalizeNotice(String notice) {
        if (notice == null) {
            return null;
        }
        String trimmed = notice.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
