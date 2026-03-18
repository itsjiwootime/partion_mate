package com.project.partition_mate.service;

import com.project.partition_mate.domain.ExternalNotificationDelivery;
import com.project.partition_mate.domain.ExternalNotificationFailureReason;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.project.partition_mate.domain.UserNotificationType;
import com.project.partition_mate.domain.WebPushSubscription;
import com.project.partition_mate.repository.WebPushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebPushNotificationService {

    private final WebPushSubscriptionRepository webPushSubscriptionRepository;
    private final WebPushGateway webPushGateway;
    private final ObjectMapper objectMapper;

    public DeliveryAttemptResult send(ExternalNotificationDelivery delivery) {
        Optional<WebPushSubscription> subscriptionOptional = webPushSubscriptionRepository.findById(delivery.getSubscriptionId());
        if (subscriptionOptional.isEmpty()) {
            return new DeliveryAttemptResult(
                    false,
                    false,
                    null,
                    ExternalNotificationFailureReason.SUBSCRIPTION_NOT_FOUND,
                    "웹 푸시 구독이 존재하지 않습니다."
            );
        }

        WebPushSubscription subscription = subscriptionOptional.get();
        try {
            WebPushGateway.DeliveryResult deliveryResult = webPushGateway.send(subscription, delivery.getPayloadJson());

            if (deliveryResult.isGone()) {
                webPushSubscriptionRepository.delete(subscription);
                return new DeliveryAttemptResult(
                        false,
                        false,
                        deliveryResult.statusCode(),
                        ExternalNotificationFailureReason.ENDPOINT_GONE,
                        "웹 푸시 endpoint가 만료되었습니다."
                );
            }

            if (deliveryResult.statusCode() >= 200 && deliveryResult.statusCode() < 300) {
                return new DeliveryAttemptResult(true, false, deliveryResult.statusCode(), null, null);
            }

            if (deliveryResult.statusCode() == 429 || deliveryResult.statusCode() >= 500) {
                return new DeliveryAttemptResult(
                        false,
                        true,
                        deliveryResult.statusCode(),
                        ExternalNotificationFailureReason.TRANSIENT_ERROR,
                        "웹 푸시 전송이 일시적으로 실패했습니다."
                );
            }

            return new DeliveryAttemptResult(
                    false,
                    false,
                    deliveryResult.statusCode(),
                    ExternalNotificationFailureReason.CLIENT_ERROR,
                    "웹 푸시 요청이 거부되었습니다."
            );
        } catch (Exception ex) {
            log.warn("Web Push delivery failed for subscriptionId={}, endpoint={}",
                    delivery.getSubscriptionId(),
                    delivery.getEndpoint(),
                    ex
            );
            return new DeliveryAttemptResult(
                    false,
                    true,
                    null,
                    ExternalNotificationFailureReason.DELIVERY_EXCEPTION,
                    ex.getMessage()
            );
        }
    }

    public String buildPayload(UserNotificationType notificationType,
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

    public record DeliveryAttemptResult(boolean success,
                                        boolean retryable,
                                        Integer statusCode,
                                        ExternalNotificationFailureReason failureReason,
                                        String errorMessage) {
    }
}
