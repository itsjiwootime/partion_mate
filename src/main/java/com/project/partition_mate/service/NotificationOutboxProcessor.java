package com.project.partition_mate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.partition_mate.domain.OutboxEvent;
import com.project.partition_mate.domain.OutboxEventStatus;
import com.project.partition_mate.domain.OutboxEventType;
import com.project.partition_mate.domain.PartyCloseReason;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.domain.UserNotification;
import com.project.partition_mate.domain.UserNotificationType;
import com.project.partition_mate.repository.OutboxEventRepository;
import com.project.partition_mate.repository.UserNotificationRepository;
import com.project.partition_mate.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationOutboxProcessor {

    private static final int RETRY_DELAY_MINUTES = 1;

    private final OutboxEventRepository outboxEventRepository;
    private final UserRepository userRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final WebPushNotificationService webPushNotificationService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public NotificationOutboxProcessor(OutboxEventRepository outboxEventRepository,
                                       UserRepository userRepository,
                                       UserNotificationRepository userNotificationRepository,
                                       WebPushNotificationService webPushNotificationService,
                                       ObjectMapper objectMapper,
                                       Clock clock) {
        this.outboxEventRepository = outboxEventRepository;
        this.userRepository = userRepository;
        this.userNotificationRepository = userNotificationRepository;
        this.webPushNotificationService = webPushNotificationService;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public int processPendingEvents() {
        return processPendingEvents(LocalDateTime.now(clock));
    }

    @Transactional
    public int processPendingEvents(LocalDateTime now) {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByIdAsc(
                OutboxEventStatus.PENDING,
                now
        );

        int processedCount = 0;
        for (OutboxEvent event : pendingEvents) {
            try {
                process(event, now);
                processedCount++;
            } catch (Exception ex) {
                event.scheduleRetry(now.plusMinutes(RETRY_DELAY_MINUTES), ex.getMessage());
            }
        }

        return processedCount;
    }

    private void process(OutboxEvent event, LocalDateTime now) throws Exception {
        JsonNode payload = objectMapper.readTree(event.getPayload());

        switch (event.getEventType()) {
            case PARTY_JOIN_CONFIRMED -> processJoinConfirmed(event, payload, now);
            case WAITING_PROMOTED -> processWaitingPromoted(event, payload, now);
            case PICKUP_UPDATED -> processPickupUpdated(event, payload, now);
            case PARTY_UPDATED -> processPartyUpdated(event, payload, now);
            case PARTY_CLOSED -> processPartyClosed(event, payload, now);
            default -> throw new IllegalStateException("지원하지 않는 Outbox 이벤트입니다.");
        }

        event.markProcessed(now);
    }

    private void processJoinConfirmed(OutboxEvent event, JsonNode payload, LocalDateTime now) {
        Long partyId = requireLong(payload, "partyId");
        Long recipientUserId = requireLong(payload, "recipientUserId");
        String partyTitle = requireText(payload, "partyTitle");

        User recipient = loadUser(recipientUserId);
        saveNotification(
                event,
                recipient,
                UserNotificationType.PARTY_JOIN_CONFIRMED,
                "파티 참여가 완료되었습니다",
                partyTitle + " 파티 참여가 확정되었습니다.",
                "/parties/" + partyId,
                now
        );
    }

    private void processWaitingPromoted(OutboxEvent event, JsonNode payload, LocalDateTime now) {
        Long partyId = requireLong(payload, "partyId");
        Long recipientUserId = requireLong(payload, "recipientUserId");
        String partyTitle = requireText(payload, "partyTitle");
        int requestedQuantity = payload.path("requestedQuantity").asInt(0);

        User recipient = loadUser(recipientUserId);
        saveNotification(
                event,
                recipient,
                UserNotificationType.WAITING_PROMOTED,
                "대기열에서 승격되었습니다",
                partyTitle + " 파티에서 " + requestedQuantity + "개 요청이 참여로 승격되었습니다.",
                "/parties/" + partyId,
                now
        );
    }

    private void processPickupUpdated(OutboxEvent event, JsonNode payload, LocalDateTime now) {
        Long partyId = requireLong(payload, "partyId");
        String partyTitle = requireText(payload, "partyTitle");
        String pickupPlace = requireText(payload, "pickupPlace");
        String pickupTime = requireText(payload, "pickupTime");
        List<Long> joinedUserIds = readLongArray(payload.path("joinedUserIds"));

        for (Long userId : joinedUserIds) {
            saveNotification(
                    event,
                    loadUser(userId),
                    UserNotificationType.PICKUP_UPDATED,
                    "픽업 일정이 확정되었습니다",
                    partyTitle + " 파티 픽업 일정이 " + pickupPlace + ", " + pickupTime + "로 확정되었습니다.",
                    "/parties/" + partyId,
                    now
            );
        }
    }

    private void processPartyUpdated(OutboxEvent event, JsonNode payload, LocalDateTime now) {
        Long partyId = requireLong(payload, "partyId");
        String partyTitle = requireText(payload, "partyTitle");
        String changeSummary = requireText(payload, "changeSummary");
        List<Long> joinedUserIds = readLongArray(payload.path("joinedUserIds"));
        List<Long> waitingUserIds = readLongArray(payload.path("waitingUserIds"));

        for (Long userId : joinedUserIds) {
            saveNotification(
                    event,
                    loadUser(userId),
                    UserNotificationType.PARTY_UPDATED,
                    "파티 조건이 변경되었습니다",
                    partyTitle + " 파티 조건이 변경되었습니다. " + changeSummary,
                    "/parties/" + partyId,
                    now
            );
        }

        for (Long userId : waitingUserIds) {
            saveNotification(
                    event,
                    loadUser(userId),
                    UserNotificationType.PARTY_UPDATED,
                    "대기 중인 파티 조건이 변경되었습니다",
                    partyTitle + " 파티 조건이 변경되었습니다. " + changeSummary,
                    "/parties/" + partyId,
                    now
            );
        }
    }

    private void processPartyClosed(OutboxEvent event, JsonNode payload, LocalDateTime now) {
        Long partyId = requireLong(payload, "partyId");
        String partyTitle = requireText(payload, "partyTitle");
        PartyCloseReason closeReason = resolveCloseReason(payload.path("closeReason").asText(null));
        List<Long> joinedUserIds = readLongArray(payload.path("joinedUserIds"));
        List<Long> expiredWaitingUserIds = readLongArray(payload.path("expiredWaitingUserIds"));

        for (Long userId : joinedUserIds) {
            saveNotification(
                    event,
                    loadUser(userId),
                    UserNotificationType.PARTY_CLOSED,
                    closeReason == PartyCloseReason.HOST_CANCELED ? "파티가 취소되었습니다" : "파티 모집이 종료되었습니다",
                    closeReason == PartyCloseReason.HOST_CANCELED
                            ? partyTitle + " 파티를 호스트가 취소했습니다."
                            : partyTitle + " 파티가 마감 시간에 도달해 종료되었습니다.",
                    "/parties/" + partyId,
                    now
            );
        }

        for (Long userId : expiredWaitingUserIds) {
            saveNotification(
                    event,
                    loadUser(userId),
                    UserNotificationType.WAITING_EXPIRED,
                    "대기열이 종료되었습니다",
                    closeReason == PartyCloseReason.HOST_CANCELED
                            ? partyTitle + " 파티를 호스트가 취소해 대기열이 종료되었습니다."
                            : partyTitle + " 파티가 종료되어 대기열이 만료되었습니다.",
                    "/parties/" + partyId,
                    now
            );
        }
    }

    private void saveNotification(OutboxEvent event,
                                  User recipient,
                                  UserNotificationType notificationType,
                                  String title,
                                  String message,
                                  String linkUrl,
                                  LocalDateTime createdAt) {
        String externalKey = externalKey(event, recipient.getId(), notificationType);
        if (userNotificationRepository.existsByExternalKey(externalKey)) {
            return;
        }

        UserNotification notification = UserNotification.create(
                recipient,
                notificationType,
                title,
                message,
                linkUrl,
                externalKey,
                createdAt
        );
        userNotificationRepository.save(notification);
        webPushNotificationService.deliver(recipient, notificationType, title, message, linkUrl);
    }

    private String externalKey(OutboxEvent event, Long userId, UserNotificationType notificationType) {
        return event.getId() + ":" + userId + ":" + notificationType.name();
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("알림 대상 사용자가 존재하지 않습니다."));
    }

    private Long requireLong(JsonNode payload, String fieldName) {
        if (!payload.hasNonNull(fieldName)) {
            throw new IllegalArgumentException(fieldName + " 필드가 payload에 없습니다.");
        }
        return payload.get(fieldName).asLong();
    }

    private String requireText(JsonNode payload, String fieldName) {
        if (!payload.hasNonNull(fieldName)) {
            throw new IllegalArgumentException(fieldName + " 필드가 payload에 없습니다.");
        }
        return payload.get(fieldName).asText();
    }

    private List<Long> readLongArray(JsonNode jsonNode) {
        List<Long> values = new ArrayList<>();
        if (jsonNode == null || !jsonNode.isArray()) {
            return values;
        }
        jsonNode.forEach(value -> values.add(value.asLong()));
        return values;
    }

    private PartyCloseReason resolveCloseReason(String value) {
        if (value == null || value.isBlank()) {
            return PartyCloseReason.DEADLINE_EXPIRED;
        }

        try {
            return PartyCloseReason.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return PartyCloseReason.DEADLINE_EXPIRED;
        }
    }
}
