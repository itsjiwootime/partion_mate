package com.project.partition_mate.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationOutboxScheduler {

    private final NotificationOutboxProcessor notificationOutboxProcessor;
    private final ExternalNotificationDeliveryService externalNotificationDeliveryService;

    public NotificationOutboxScheduler(NotificationOutboxProcessor notificationOutboxProcessor,
                                       ExternalNotificationDeliveryService externalNotificationDeliveryService) {
        this.notificationOutboxProcessor = notificationOutboxProcessor;
        this.externalNotificationDeliveryService = externalNotificationDeliveryService;
    }

    @Scheduled(fixedDelayString = "${partition.notification.outbox-delay-ms:10000}")
    public void processPendingNotifications() {
        notificationOutboxProcessor.processPendingEvents();
    }

    @Scheduled(fixedDelayString = "${partition.notification.external-delay-ms:10000}")
    public void processPendingExternalNotifications() {
        externalNotificationDeliveryService.processPendingDeliveries();
    }
}
