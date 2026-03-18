package com.project.partition_mate.domain;

public enum ExternalNotificationFailureReason {
    SUBSCRIPTION_NOT_FOUND,
    ENDPOINT_GONE,
    CLIENT_ERROR,
    TRANSIENT_ERROR,
    DELIVERY_EXCEPTION
}
