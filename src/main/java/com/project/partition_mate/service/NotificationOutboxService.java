package com.project.partition_mate.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.project.partition_mate.domain.OutboxEvent;
import com.project.partition_mate.domain.OutboxEventType;
import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyCloseReason;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationOutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public NotificationOutboxService(OutboxEventRepository outboxEventRepository,
                                     ObjectMapper objectMapper,
                                     Clock clock) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void publishJoinConfirmed(Party party, User recipient) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("partyId", party.getId());
        payload.put("partyTitle", party.getTitle());
        payload.put("recipientUserId", recipient.getId());

        save("PARTY", party.getId(), OutboxEventType.PARTY_JOIN_CONFIRMED, payload);
    }

    public void publishWaitingPromoted(Party party, User recipient, int requestedQuantity) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("partyId", party.getId());
        payload.put("partyTitle", party.getTitle());
        payload.put("recipientUserId", recipient.getId());
        payload.put("requestedQuantity", requestedQuantity);

        save("PARTY", party.getId(), OutboxEventType.WAITING_PROMOTED, payload);
    }

    public void publishPartyClosed(Party party, List<Long> joinedUserIds, List<Long> expiredWaitingUserIds) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("partyId", party.getId());
        payload.put("partyTitle", party.getTitle());
        payload.put("closeReason", party.getCloseReason() != null ? party.getCloseReason().name() : PartyCloseReason.DEADLINE_EXPIRED.name());
        payload.set("joinedUserIds", toLongArray(joinedUserIds));
        payload.set("expiredWaitingUserIds", toLongArray(expiredWaitingUserIds));

        save("PARTY", party.getId(), OutboxEventType.PARTY_CLOSED, payload);
    }

    private ArrayNode toLongArray(List<Long> values) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        values.forEach(arrayNode::add);
        return arrayNode;
    }

    private void save(String aggregateType, Long aggregateId, OutboxEventType eventType, ObjectNode payload) {
        try {
            OutboxEvent event = OutboxEvent.create(
                    aggregateType,
                    aggregateId,
                    eventType,
                    objectMapper.writeValueAsString(payload),
                    LocalDateTime.now(clock)
            );
            outboxEventRepository.save(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Outbox payload 직렬화에 실패했습니다.", e);
        }
    }
}
