package com.project.partition_mate.service;

import com.project.partition_mate.domain.ExternalNotificationChannel;
import com.project.partition_mate.domain.ExternalNotificationDelivery;
import com.project.partition_mate.domain.ExternalNotificationDeliveryStatus;
import com.project.partition_mate.domain.OutboxEvent;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.domain.UserNotification;
import com.project.partition_mate.domain.UserNotificationType;
import com.project.partition_mate.domain.WebPushSubscription;
import com.project.partition_mate.repository.ExternalNotificationDeliveryRepository;
import com.project.partition_mate.repository.WebPushSubscriptionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExternalNotificationDeliveryService {

    private final ExternalNotificationDeliveryRepository externalNotificationDeliveryRepository;
    private final WebPushSubscriptionRepository webPushSubscriptionRepository;
    private final UserNotificationPreferenceService userNotificationPreferenceService;
    private final WebPushNotificationService webPushNotificationService;
    private final Clock clock;

    @Transactional
    public void enqueueWebPushDeliveries(OutboxEvent outboxEvent,
                                         UserNotification notification,
                                         User recipient,
                                         UserNotificationType notificationType,
                                         String title,
                                         String message,
                                         String linkUrl,
                                         LocalDateTime queuedAt) {
        if (!notificationType.supportsWebPush()) {
            return;
        }

        if (!userNotificationPreferenceService.isWebPushEnabled(recipient, notificationType)) {
            return;
        }

        List<WebPushSubscription> subscriptions = webPushSubscriptionRepository.findAllByUserOrderByUpdatedAtDesc(recipient);
        if (subscriptions.isEmpty()) {
            return;
        }

        String payloadJson = webPushNotificationService.buildPayload(notificationType, title, message, linkUrl);

        for (WebPushSubscription subscription : subscriptions) {
            String deliveryKey = deliveryKey(outboxEvent, notification, subscription);
            if (externalNotificationDeliveryRepository.existsByDeliveryKey(deliveryKey)) {
                continue;
            }

            externalNotificationDeliveryRepository.save(
                    ExternalNotificationDelivery.create(
                            outboxEvent.getId(),
                            notification.getId(),
                            recipient.getId(),
                            ExternalNotificationChannel.WEB_PUSH,
                            notificationType,
                            subscription.getId(),
                            subscription.getEndpoint(),
                            deliveryKey,
                            payloadJson,
                            queuedAt
                    )
            );
        }
    }

    @Transactional
    public int processPendingDeliveries() {
        return processPendingDeliveries(LocalDateTime.now(clock));
    }

    @Transactional
    public int processPendingDeliveries(LocalDateTime now) {
        List<ExternalNotificationDelivery> pendingDeliveries =
                externalNotificationDeliveryRepository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByIdAsc(
                        ExternalNotificationDeliveryStatus.PENDING,
                        now
                );

        int processedCount = 0;
        for (ExternalNotificationDelivery delivery : pendingDeliveries) {
            process(delivery, now);
            processedCount++;
        }

        return processedCount;
    }

    private void process(ExternalNotificationDelivery delivery, LocalDateTime now) {
        WebPushNotificationService.DeliveryAttemptResult result = webPushNotificationService.send(delivery);

        if (result.success()) {
            delivery.markSent(now, result.statusCode() != null ? result.statusCode() : 201);
            return;
        }

        if (result.retryable() && delivery.getAttemptCount() + 1 < 3) {
            delivery.scheduleRetry(
                    now,
                    now.plusMinutes(retryDelayMinutes(delivery.getAttemptCount() + 1)),
                    result.statusCode(),
                    result.failureReason(),
                    result.errorMessage()
            );
            return;
        }

        delivery.markFailed(now, result.statusCode(), result.failureReason(), result.errorMessage());
    }

    private long retryDelayMinutes(int nextAttemptNumber) {
        return switch (nextAttemptNumber) {
            case 1 -> 1;
            case 2 -> 5;
            default -> 15;
        };
    }

    private String deliveryKey(OutboxEvent outboxEvent,
                               UserNotification notification,
                               WebPushSubscription subscription) {
        return outboxEvent.getId()
                + ":" + notification.getId()
                + ":" + ExternalNotificationChannel.WEB_PUSH.name()
                + ":" + subscription.getId();
    }
}
