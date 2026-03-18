package com.project.partition_mate.service;

import com.project.partition_mate.domain.WebPushSubscription;

public interface WebPushGateway {

    DeliveryResult send(WebPushSubscription subscription, String payloadJson) throws Exception;

    record DeliveryResult(int statusCode) {

        public boolean isGone() {
            return statusCode == 404 || statusCode == 410;
        }
    }
}
