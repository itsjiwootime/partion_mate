package com.project.partition_mate.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationOutboxScheduler {

    private final NotificationOutboxProcessor notificationOutboxProcessor;

    public NotificationOutboxScheduler(NotificationOutboxProcessor notificationOutboxProcessor) {
        this.notificationOutboxProcessor = notificationOutboxProcessor;
    }

    @Scheduled(fixedDelayString = "${partition.notification.outbox-delay-ms:10000}")
    public void processPendingNotifications() {
        notificationOutboxProcessor.processPendingEvents();
    }
}
