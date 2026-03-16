package com.project.partition_mate.service;

import com.project.partition_mate.domain.WaitingQueueEntry;
import com.project.partition_mate.domain.WaitingQueueStatus;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.UserNotificationResponse;
import com.project.partition_mate.dto.MyJoinedPartyResponse;
import com.project.partition_mate.repository.PartyMemberRepository;
import com.project.partition_mate.repository.UserNotificationRepository;
import com.project.partition_mate.repository.UserRepository;
import com.project.partition_mate.repository.WaitingQueueRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final WaitingQueueRepository waitingQueueRepository;
    private final UserNotificationRepository userNotificationRepository;

    public User getUserByEmail(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("해당 이메일을 가진 사용자를  찾을 수 없습니다."));

        return user;
    }

    public List<MyJoinedPartyResponse> getMyParties(User user) {
        List<MyJoinedPartyResponse> responses = new ArrayList<>();

        responses.addAll(partyMemberRepository.findByUser(user).stream()
                .map(pm -> MyJoinedPartyResponse.joined(
                        pm.getParty().getId(),
                        pm.getParty().getTitle(),
                        pm.getParty().getProductName(),
                        pm.getParty().getStore() != null ? pm.getParty().getStore().getName() : null,
                        pm.getParty().getPartyStatus(),
                        pm.getParty().getTotalQuantity(),
                        pm.getParty().getRequestedQuantity(),
                        pm.getRole(),
                        pm.getParty().getTotalPrice(),
                        pm.getParty().getOpenChatUrl(),
                        pm.getRequestedQuantity(),
                        pm.getParty().getDeadline(),
                        pm.getParty().getClosedAt(),
                        pm.getParty().getCloseReason()
                ))
                .toList());

        responses.addAll(waitingQueueRepository.findAllByUserAndStatusOrderByQueuedAtDesc(user, WaitingQueueStatus.WAITING).stream()
                .map(this::toWaitingResponse)
                .toList());

        return responses;
    }

    private MyJoinedPartyResponse toWaitingResponse(WaitingQueueEntry waitingQueueEntry) {
        List<WaitingQueueEntry> waitingEntries = waitingQueueRepository.findAllByPartyAndStatusOrderByQueuedAtAsc(
                waitingQueueEntry.getParty(),
                WaitingQueueStatus.WAITING
        );

        int waitingPosition = 1;
        for (WaitingQueueEntry currentEntry : waitingEntries) {
            if (currentEntry.getId().equals(waitingQueueEntry.getId())) {
                break;
            }
            waitingPosition++;
        }

        return MyJoinedPartyResponse.waiting(
                waitingQueueEntry.getParty().getId(),
                waitingQueueEntry.getParty().getTitle(),
                waitingQueueEntry.getParty().getProductName(),
                waitingQueueEntry.getParty().getStore() != null ? waitingQueueEntry.getParty().getStore().getName() : null,
                waitingQueueEntry.getParty().getPartyStatus(),
                waitingQueueEntry.getParty().getTotalQuantity(),
                waitingQueueEntry.getParty().getRequestedQuantity(),
                waitingQueueEntry.getParty().getTotalPrice(),
                waitingQueueEntry.getParty().getOpenChatUrl(),
                waitingPosition,
                waitingQueueEntry.getRequestedQuantity(),
                waitingQueueEntry.getParty().getDeadline(),
                waitingQueueEntry.getParty().getClosedAt(),
                waitingQueueEntry.getParty().getCloseReason()
        );
    }

    public List<UserNotificationResponse> getNotifications(User user) {
        return userNotificationRepository.findAllByUserOrderByCreatedAtDesc(user).stream()
                .map(UserNotificationResponse::from)
                .toList();
    }
}
