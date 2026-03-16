package com.project.partition_mate.dto;

import com.project.partition_mate.domain.UserNotification;
import com.project.partition_mate.domain.UserNotificationType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserNotificationResponse {

    private final Long id;
    private final UserNotificationType type;
    private final String title;
    private final String message;
    private final String linkUrl;
    private final LocalDateTime createdAt;
    private final String createdAtLabel;
    private final boolean read;

    private UserNotificationResponse(Long id,
                                     UserNotificationType type,
                                     String title,
                                     String message,
                                     String linkUrl,
                                     LocalDateTime createdAt,
                                     boolean read) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.message = message;
        this.linkUrl = linkUrl;
        this.createdAt = createdAt;
        this.createdAtLabel = DateTimeLabelFormatter.format(createdAt);
        this.read = read;
    }

    public static UserNotificationResponse from(UserNotification notification) {
        return new UserNotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getLinkUrl(),
                notification.getCreatedAt(),
                notification.isRead()
        );
    }
}
