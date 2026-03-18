package com.project.partition_mate.service;

import com.project.partition_mate.domain.ChatMessage;
import com.project.partition_mate.domain.ChatRoom;
import com.project.partition_mate.domain.ChatReadState;
import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.ChatMessageRequest;
import com.project.partition_mate.dto.ChatMessageResponse;
import com.project.partition_mate.dto.ChatReadReceiptResponse;
import com.project.partition_mate.dto.ChatRoomDetailResponse;
import com.project.partition_mate.dto.ChatRoomSummaryResponse;
import com.project.partition_mate.dto.UpdatePinnedNoticeRequest;
import com.project.partition_mate.exception.BusinessException;
import com.project.partition_mate.repository.ChatMessageRepository;
import com.project.partition_mate.repository.ChatReadStateRepository;
import com.project.partition_mate.repository.ChatRoomRepository;
import com.project.partition_mate.repository.PartyMemberRepository;
import com.project.partition_mate.repository.PartyRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatReadStateRepository chatReadStateRepository;
    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final UserBlockPolicyService userBlockPolicyService;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final Clock clock;

    @Transactional
    public void initializePartyChatRoom(Party party, User host) {
        ChatRoom chatRoom = findOrCreateRoom(party);
        if (chatMessageRepository.countByChatRoom(chatRoom) > 0) {
            return;
        }

        ChatMessage message = chatMessageRepository.save(ChatMessage.system(
                chatRoom,
                host.getUsername() + "님이 파티 채팅방을 열었습니다.",
                LocalDateTime.now(clock)
        ));
        markReadInternal(chatRoom, host, message.getId());
        publishAfterCommit(message, null);
    }

    @Transactional
    public void appendSystemMessage(Party party, String content) {
        appendSystemMessage(party, content, null);
    }

    @Transactional
    public void appendSystemMessage(Party party, String content, String pinnedNoticeSnapshot) {
        ChatRoom chatRoom = findOrCreateRoom(party);
        ChatMessage message = chatMessageRepository.save(ChatMessage.system(
                chatRoom,
                content,
                LocalDateTime.now(clock)
        ));
        publishAfterCommit(message, pinnedNoticeSnapshot);
    }

    @Transactional
    public void sendTextMessage(Long partyId, ChatMessageRequest request, User user) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new EntityNotFoundException("파티가 존재하지 않습니다."));
        PartyMember partyMember = getRequiredPartyMember(party, user);
        validateBlockedChatAccess(party, user);
        ChatRoom chatRoom = findOrCreateRoom(party);
        ChatMessage message = chatMessageRepository.save(ChatMessage.text(
                chatRoom,
                partyMember.getUser(),
                request.getContent(),
                LocalDateTime.now(clock)
        ));
        markReadInternal(chatRoom, user, message.getId());
        publishAfterCommit(message, null);
    }

    @Transactional
    public ChatRoomDetailResponse getChatRoomDetail(Long partyId, User user) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new EntityNotFoundException("파티가 존재하지 않습니다."));
        PartyMember partyMember = getRequiredPartyMember(party, user);
        validateBlockedChatAccess(party, user);
        ChatRoom chatRoom = findOrCreateRoom(party);
        markReadInternal(chatRoom, user, latestMessageId(chatRoom));
        return toDetailResponse(chatRoom, partyMember.isHost(), user.getId(), 0);
    }

    @Transactional
    public ChatReadReceiptResponse markRoomRead(Long partyId, User user) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new EntityNotFoundException("파티가 존재하지 않습니다."));
        getRequiredPartyMember(party, user);
        validateBlockedChatAccess(party, user);
        ChatRoom chatRoom = findOrCreateRoom(party);
        markReadInternal(chatRoom, user, latestMessageId(chatRoom));
        return new ChatReadReceiptResponse(partyId, 0);
    }

    @Transactional
    public ChatRoomDetailResponse updatePinnedNotice(Long partyId, UpdatePinnedNoticeRequest request, User user) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new EntityNotFoundException("파티가 존재하지 않습니다."));
        PartyMember partyMember = getRequiredPartyMember(party, user);
        validateBlockedChatAccess(party, user);
        if (!partyMember.isHost()) {
            throw BusinessException.onlyHostCanManageChatNotice();
        }

        ChatRoom chatRoom = findOrCreateRoom(party);
        String nextNotice = request.getPinnedNotice();
        boolean changed = chatRoom.updatePinnedNotice(nextNotice, LocalDateTime.now(clock));
        if (changed) {
            String systemMessage = chatRoom.getPinnedNotice() == null
                    ? "호스트 공지가 해제되었습니다."
                    : "호스트 공지가 업데이트되었습니다.";
            ChatMessage message = chatMessageRepository.save(ChatMessage.system(
                    chatRoom,
                    systemMessage,
                    LocalDateTime.now(clock)
            ));
            markReadInternal(chatRoom, user, message.getId());
            publishAfterCommit(message, chatRoom.getPinnedNotice() == null ? "" : chatRoom.getPinnedNotice());
        }

        return toDetailResponse(chatRoom, true, user.getId(), resolveUnreadCount(chatRoom, user));
    }

    @Transactional
    public List<ChatRoomSummaryResponse> getMyChatRooms(User user) {
        return partyMemberRepository.findByUser(user).stream()
                .map(PartyMember::getParty)
                .distinct()
                .map(party -> toSummaryResponse(findOrCreateRoom(party), user))
                .sorted(Comparator
                        .comparing(ChatRoomSummaryResponse::getLastMessageCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ChatRoomSummaryResponse::getPartyId, Comparator.reverseOrder()))
                .toList();
    }

    private ChatRoomSummaryResponse toSummaryResponse(ChatRoom chatRoom, User user) {
        ChatMessage lastMessage = chatMessageRepository.findTopByChatRoomOrderByIdDesc(chatRoom).orElse(null);
        return new ChatRoomSummaryResponse(
                chatRoom.getId(),
                chatRoom.getParty().getId(),
                chatRoom.getParty().getTitle(),
                chatRoom.getParty().getStore() != null ? chatRoom.getParty().getStore().getName() : null,
                chatRoom.getPinnedNotice(),
                summarize(lastMessage),
                lastMessage != null ? lastMessage.getType().name() : null,
                lastMessage != null ? lastMessage.getCreatedAt() : null,
                resolveUnreadCount(chatRoom, user)
        );
    }

    private ChatRoomDetailResponse toDetailResponse(ChatRoom chatRoom,
                                                    boolean host,
                                                    Long currentUserId,
                                                    long unreadCount) {
        List<ChatMessageResponse> messages = chatMessageRepository.findTop100ByChatRoomOrderByIdDesc(chatRoom).stream()
                .sorted(Comparator.comparing(ChatMessage::getId))
                .map(message -> ChatMessageResponse.from(message, currentUserId))
                .toList();

        return new ChatRoomDetailResponse(
                chatRoom.getId(),
                chatRoom.getParty().getId(),
                chatRoom.getParty().getTitle(),
                chatRoom.getParty().getStore() != null ? chatRoom.getParty().getStore().getName() : null,
                host,
                chatRoom.getPinnedNotice(),
                chatRoom.getPinnedNoticeUpdatedAt(),
                unreadCount,
                messages
        );
    }

    private String summarize(ChatMessage lastMessage) {
        if (lastMessage == null) {
            return null;
        }
        String content = lastMessage.getContent();
        if (content.length() <= 40) {
            return content;
        }
        return content.substring(0, 40) + "...";
    }

    private long resolveUnreadCount(ChatRoom chatRoom, User user) {
        ChatReadState readState = chatReadStateRepository.findByChatRoomAndUser(chatRoom, user).orElse(null);
        if (readState == null || readState.getLastReadMessageId() == null) {
            return chatMessageRepository.countByChatRoom(chatRoom);
        }
        return chatMessageRepository.countByChatRoomAndIdGreaterThan(chatRoom, readState.getLastReadMessageId());
    }

    private Long latestMessageId(ChatRoom chatRoom) {
        return chatMessageRepository.findTopByChatRoomOrderByIdDesc(chatRoom)
                .map(ChatMessage::getId)
                .orElse(null);
    }

    private void markReadInternal(ChatRoom chatRoom, User user, Long lastMessageId) {
        if (lastMessageId == null) {
            return;
        }

        ChatReadState readState = chatReadStateRepository.findByChatRoomAndUser(chatRoom, user)
                .orElseGet(() -> ChatReadState.create(chatRoom, user));
        readState.markRead(lastMessageId, LocalDateTime.now(clock));
        chatReadStateRepository.save(readState);
    }

    private PartyMember getRequiredPartyMember(Party party, User user) {
        return partyMemberRepository.findByPartyAndUser(party, user)
                .orElseThrow(() -> new EntityNotFoundException("파티 참여자만 채팅을 사용할 수 있습니다."));
    }

    private void validateBlockedChatAccess(Party party, User user) {
        if (userBlockPolicyService.hasBlockedParticipantInParty(party, user.getId())) {
            throw BusinessException.blockedChatAccessNotAllowed();
        }
    }

    private ChatRoom findOrCreateRoom(Party party) {
        return chatRoomRepository.findByParty(party)
                .orElseGet(() -> chatRoomRepository.save(ChatRoom.create(party, LocalDateTime.now(clock))));
    }

    private void publishAfterCommit(ChatMessage message, String pinnedNoticeSnapshot) {
        ChatMessageResponse response = pinnedNoticeSnapshot == null
                ? ChatMessageResponse.from(message, null)
                : ChatMessageResponse.from(message, null, pinnedNoticeSnapshot);

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    simpMessagingTemplate.convertAndSend("/topic/chat/" + message.getChatRoom().getParty().getId(), response);
                }
            });
            return;
        }

        simpMessagingTemplate.convertAndSend("/topic/chat/" + message.getChatRoom().getParty().getId(), response);
    }
}
