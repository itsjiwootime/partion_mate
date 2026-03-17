package com.project.partition_mate.dto;

import com.project.partition_mate.domain.UserBlock;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserBlockResponse {

    private final Long id;
    private final Long targetUserId;
    private final String targetUsername;
    private final LocalDateTime createdAt;
    private final String createdAtLabel;

    private UserBlockResponse(Long id,
                              Long targetUserId,
                              String targetUsername,
                              LocalDateTime createdAt) {
        this.id = id;
        this.targetUserId = targetUserId;
        this.targetUsername = targetUsername;
        this.createdAt = createdAt;
        this.createdAtLabel = DateTimeLabelFormatter.format(createdAt);
    }

    public static UserBlockResponse from(UserBlock userBlock) {
        return new UserBlockResponse(
                userBlock.getId(),
                userBlock.getBlocked().getId(),
                userBlock.getBlocked().getUsername(),
                userBlock.getCreatedAt()
        );
    }
}
