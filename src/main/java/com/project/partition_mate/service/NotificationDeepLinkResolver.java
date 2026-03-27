package com.project.partition_mate.service;

import com.project.partition_mate.domain.UserNotificationType;
import org.springframework.stereotype.Component;

@Component
public class NotificationDeepLinkResolver {

    public String resolve(UserNotificationType notificationType, Long partyId) {
        if (partyId == null) {
            return "/notifications";
        }

        return switch (notificationType) {
            case PARTY_JOIN_CONFIRMED -> "/chat/" + partyId;
            case PICKUP_UPDATED, PARTY_UPDATED -> "/parties/" + partyId;
            case PARTY_CLOSED -> "/notifications";
        };
    }
}
