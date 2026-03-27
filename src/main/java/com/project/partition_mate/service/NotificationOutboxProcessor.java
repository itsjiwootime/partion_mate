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
    private static final int MAX_RETRY_COUNT = 3;

    private final OutboxEventRepository outboxEventRepository;
    private final UserRepository userRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final ExternalNotificationDeliveryService externalNotificationDeliveryService;
    private final NotificationDeepLinkResolver notificationDeepLinkResolver;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public NotificationOutboxProcessor(OutboxEventRepository outboxEventRepository,
                                       UserRepository userRepository,
                                       UserNotificationRepository userNotificationRepository,
                                       ExternalNotificationDeliveryService externalNotificationDeliveryService,
                                       NotificationDeepLinkResolver notificationDeepLinkResolver,
                                       ObjectMapper objectMapper,
                                       Clock clock) {
        this.outboxEventRepository = outboxEventRepository;
        this.userRepository = userRepository;
        this.userNotificationRepository = userNotificationRepository;
        this.externalNotificationDeliveryService = externalNotificationDeliveryService;
        this.notificationDeepLinkResolver = notificationDeepLinkResolver;
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
                if (event.getRetryCount() + 1 >= MAX_RETRY_COUNT) {
                    event.markFailed(now, ex.getMessage());
                } else {
                    event.scheduleRetry(now.plusMinutes(RETRY_DELAY_MINUTES), ex.getMessage());
                }
            }
        }

        return processedCount;
    }

    private void process(OutboxEvent event, LocalDateTime now) throws Exception {
        JsonNode payload = objectMapper.readTree(event.getPayload());

        switch (event.getEventType()) {
            case PARTY_JOIN_CONFIRMED -> processJoinConfirmed(event, payload, now);
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
                notificationDeepLinkResolver.resolve(UserNotificationType.PARTY_JOIN_CONFIRMED, partyId),
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
                    notificationDeepLinkResolver.resolve(UserNotificationType.PICKUP_UPDATED, partyId),
                    now
            );
        }
    }

    private void processPartyUpdated(OutboxEvent event, JsonNode payload, LocalDateTime now) {
        Long partyId = requireLong(payload, "partyId");
        String partyTitle = requireText(payload, "partyTitle");
        String changeSummary = requireText(payload, "changeSummary");
        List<Long> joinedUserIds = readLongArray(payload.path("joinedUserIds"));

        for (Long userId : joinedUserIds) {
            saveNotification(
                    event,
                    loadUser(userId),
                    UserNotificationType.PARTY_UPDATED,
                    "파티 조건이 변경되었습니다",
                    partyTitle + " 파티 조건이 변경되었습니다. " + changeSummary,
                    notificationDeepLinkResolver.resolve(UserNotificationType.PARTY_UPDATED, partyId),
                    now
            );
        }
    }

    private void processPartyClosed(OutboxEvent event, JsonNode payload, LocalDateTime now) {
        Long partyId = requireLong(payload, "partyId");
        String partyTitle = requireText(payload, "partyTitle");
        PartyCloseReason closeReason = resolveCloseReason(payload.path("closeReason").asText(null));
        List<Long> joinedUserIds = readLongArray(payload.path("joinedUserIds"));

        for (Long userId : joinedUserIds) {
            saveNotification(
                    event,
                    loadUser(userId),
                    UserNotificationType.PARTY_CLOSED,
                    closeReason == PartyCloseReason.HOST_CANCELED ? "파티가 취소되었습니다" : "파티 모집이 종료되었습니다",
                    closeReason == PartyCloseReason.HOST_CANCELED
                            ? partyTitle + " 파티를 호스트가 취소했습니다."
                            : partyTitle + " 파티가 마감 시간에 도달해 종료되었습니다.",
                    notificationDeepLinkResolver.resolve(UserNotificationType.PARTY_CLOSED, partyId),
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
        UserNotification notification = userNotificationRepository.findByExternalKey(externalKey)
                .orElseGet(() -> userNotificationRepository.save(
                        UserNotification.create(
                                recipient,
                                notificationType,
                                title,
                                message,
                                linkUrl,
                                externalKey,
                                createdAt
                        )
                ));

        externalNotificationDeliveryService.enqueueWebPushDeliveries(
                event,
                notification,
                recipient,
                notificationType,
                title,
                message,
                linkUrl,
                createdAt
        );
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
