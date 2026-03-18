package com.project.partition_mate.dto;

import com.project.partition_mate.domain.UserNotificationType;
import lombok.Getter;

@Getter
public class NotificationPreferenceResponse {

    private final UserNotificationType type;
    private final String label;
    private final String description;
    private final String deepLinkTargetLabel;
    private final boolean webPushSupported;
    private final boolean webPushEnabled;

    private NotificationPreferenceResponse(UserNotificationType type,
                                           boolean webPushEnabled) {
        this.type = type;
        this.label = type.getDisplayLabel();
        this.description = type.getDescription();
        this.deepLinkTargetLabel = type.getDeepLinkTargetLabel();
        this.webPushSupported = type.supportsWebPush();
        this.webPushEnabled = webPushEnabled;
    }

    public static NotificationPreferenceResponse of(UserNotificationType type, boolean webPushEnabled) {
        return new NotificationPreferenceResponse(type, webPushEnabled);
    }
}
