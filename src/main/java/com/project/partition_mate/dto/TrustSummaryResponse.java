package com.project.partition_mate.dto;

import com.project.partition_mate.domain.TrustLevel;
import lombok.Getter;

@Getter
public class TrustSummaryResponse {

    private final Long userId;
    private final String username;
    private final double averageRating;
    private final long reviewCount;
    private final long completedTradeCount;
    private final long noShowCount;
    private final int completionRate;
    private final int trustScore;
    private final TrustLevel trustLevel;
    private final String trustLevelLabel;

    public TrustSummaryResponse(Long userId,
                                String username,
                                double averageRating,
                                long reviewCount,
                                long completedTradeCount,
                                long noShowCount,
                                int completionRate,
                                int trustScore,
                                TrustLevel trustLevel) {
        this.userId = userId;
        this.username = username;
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
        this.completedTradeCount = completedTradeCount;
        this.noShowCount = noShowCount;
        this.completionRate = completionRate;
        this.trustScore = trustScore;
        this.trustLevel = trustLevel;
        this.trustLevelLabel = trustLevel != null ? trustLevel.getLabel() : null;
    }
}
