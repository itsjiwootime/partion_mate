package com.project.partition_mate.dto;

import com.project.partition_mate.domain.WebPushSubscription;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class WebPushSubscriptionResponse {

    private final Long id;
    private final String endpoint;
    private final LocalDateTime createdAt;
    private final String createdAtLabel;
    private final LocalDateTime updatedAt;
    private final String updatedAtLabel;

    private WebPushSubscriptionResponse(Long id,
                                        String endpoint,
                                        LocalDateTime createdAt,
                                        LocalDateTime updatedAt) {
        this.id = id;
        this.endpoint = endpoint;
        this.createdAt = createdAt;
        this.createdAtLabel = DateTimeLabelFormatter.format(createdAt);
        this.updatedAt = updatedAt;
        this.updatedAtLabel = DateTimeLabelFormatter.format(updatedAt);
    }

    public static WebPushSubscriptionResponse from(WebPushSubscription subscription) {
        return new WebPushSubscriptionResponse(
                subscription.getId(),
                subscription.getEndpoint(),
                subscription.getCreatedAt(),
                subscription.getUpdatedAt()
        );
    }
}
