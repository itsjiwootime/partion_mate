package com.project.partition_mate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.domain.UserNotificationType;
import com.project.partition_mate.domain.WebPushSubscription;
import com.project.partition_mate.repository.WebPushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebPushNotificationService {

    private final WebPushSubscriptionRepository webPushSubscriptionRepository;
    private final UserNotificationPreferenceService userNotificationPreferenceService;
    private final WebPushGateway webPushGateway;
    private final ObjectMapper objectMapper;

    public void deliver(User recipient,
                        UserNotificationType notificationType,
                        String title,
                        String message,
                        String linkUrl) {
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

        String payload = buildPayload(notificationType, title, message, linkUrl);
        for (WebPushSubscription subscription : subscriptions) {
            try {
                WebPushGateway.DeliveryResult deliveryResult = webPushGateway.send(subscription, payload);
                if (deliveryResult.isGone()) {
                    webPushSubscriptionRepository.delete(subscription);
                }
            } catch (Exception ex) {
                log.warn("Web Push delivery failed for subscriptionId={}, userId={}, type={}",
                        subscription.getId(),
                        recipient.getId(),
                        notificationType,
                        ex
                );
            }
        }
    }

    private String buildPayload(UserNotificationType notificationType,
                                String title,
                                String message,
                                String linkUrl) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", notificationType.name());
        payload.put("title", title);
        payload.put("body", message);
        payload.put("url", linkUrl);
        payload.put("tag", notificationType.name());
        return payload.toString();
    }
}
