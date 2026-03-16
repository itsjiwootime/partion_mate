package com.project.partition_mate.service;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.dto.PartyRealtimeEventResponse;
import com.project.partition_mate.dto.PartyRealtimeTrigger;
import com.project.partition_mate.dto.PartyStreamConnectedResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PartyRealtimeService {

    private static final long SSE_TIMEOUT_MS = 30L * 60L * 1000L;
    private static final long RECONNECT_DELAY_MS = 3000L;

    private final Map<String, PartyStreamSubscription> subscriptions = new ConcurrentHashMap<>();
    private final Clock clock;

    public PartyRealtimeService(Clock clock) {
        this.clock = clock;
    }

    public SseEmitter subscribe(Long storeId, Long partyId) {
        String streamId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        PartyStreamSubscription subscription = new PartyStreamSubscription(streamId, emitter, storeId, partyId);

        subscriptions.put(streamId, subscription);
        emitter.onCompletion(() -> remove(streamId));
        emitter.onTimeout(() -> remove(streamId));
        emitter.onError(ex -> remove(streamId));

        sendConnected(subscription);
        return emitter;
    }

    public void publishPartyUpdatedAfterCommit(Party party, PartyRealtimeTrigger realtimeTrigger) {
        PartyRealtimeEventResponse event = PartyRealtimeEventResponse.from(
                party,
                realtimeTrigger,
                LocalDateTime.now(clock)
        );

        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    broadcast(event);
                }
            });
            return;
        }

        broadcast(event);
    }

    private void sendConnected(PartyStreamSubscription subscription) {
        try {
            subscription.emitter().send(SseEmitter.event()
                    .id(subscription.streamId())
                    .name("connected")
                    .reconnectTime(RECONNECT_DELAY_MS)
                    .data(PartyStreamConnectedResponse.of(
                            subscription.streamId(),
                            LocalDateTime.now(clock),
                            RECONNECT_DELAY_MS
                    )));
        } catch (IOException ex) {
            remove(subscription.streamId());
        }
    }

    private void broadcast(PartyRealtimeEventResponse event) {
        subscriptions.values().forEach(subscription -> {
            if (!matches(subscription, event)) {
                return;
            }

            try {
                subscription.emitter().send(SseEmitter.event()
                        .id(event.getEventId())
                        .name("party-updated")
                        .reconnectTime(RECONNECT_DELAY_MS)
                        .data(event));
            } catch (IOException ex) {
                remove(subscription.streamId());
            }
        });
    }

    private boolean matches(PartyStreamSubscription subscription, PartyRealtimeEventResponse event) {
        if (subscription.storeId() != null && !subscription.storeId().equals(event.getStoreId())) {
            return false;
        }

        return subscription.partyId() == null || subscription.partyId().equals(event.getId());
    }

    private void remove(String streamId) {
        subscriptions.remove(streamId);
    }

    private record PartyStreamSubscription(
            String streamId,
            SseEmitter emitter,
            Long storeId,
            Long partyId
    ) {
    }
}
