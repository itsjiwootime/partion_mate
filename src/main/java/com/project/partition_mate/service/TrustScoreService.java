package com.project.partition_mate.service;

import com.project.partition_mate.domain.PartyMemberRole;
import com.project.partition_mate.domain.TradeStatus;
import com.project.partition_mate.domain.TrustLevel;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.ReviewResponse;
import com.project.partition_mate.dto.TrustSummaryResponse;
import com.project.partition_mate.repository.PartyMemberRepository;
import com.project.partition_mate.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TrustScoreService {

    private final ReviewRepository reviewRepository;
    private final PartyMemberRepository partyMemberRepository;

    public TrustSummaryResponse getTrustSummary(User user) {
        long reviewCount = reviewRepository.countByReviewee(user);
        double averageRating = roundAverage(reviewRepository.findAverageRatingByReviewee(user));

        long completedAsMember = partyMemberRepository.countByUserAndRoleAndTradeStatus(
                user,
                PartyMemberRole.MEMBER,
                TradeStatus.COMPLETED
        );
        long completedAsHost = partyMemberRepository.countHostedMemberTradesByStatus(user.getId(), TradeStatus.COMPLETED);
        long noShowCount = partyMemberRepository.countByUserAndRoleAndTradeStatus(
                user,
                PartyMemberRole.MEMBER,
                TradeStatus.NO_SHOW
        );

        long completedTradeCount = completedAsMember + completedAsHost;
        long totalDecidedTrades = completedTradeCount + noShowCount;
        int completionRate = totalDecidedTrades == 0
                ? 0
                : (int) Math.round((completedTradeCount * 100.0) / totalDecidedTrades);

        int trustScore = calculateTrustScore(averageRating, completionRate, noShowCount, reviewCount);
        TrustLevel trustLevel = resolveTrustLevel(reviewCount, completedTradeCount, noShowCount, averageRating, trustScore);

        return new TrustSummaryResponse(
                user.getId(),
                user.getUsername(),
                averageRating,
                reviewCount,
                completedTradeCount,
                noShowCount,
                completionRate,
                trustScore,
                trustLevel
        );
    }

    public List<ReviewResponse> getRecentReviews(User user, int limit) {
        return reviewRepository.findTop5ByRevieweeOrderByCreatedAtDesc(user).stream()
                .limit(limit)
                .map(ReviewResponse::from)
                .toList();
    }

    private double roundAverage(Double averageRating) {
        if (averageRating == null) {
            return 0.0;
        }
        return Math.round(averageRating * 10.0) / 10.0;
    }

    private int calculateTrustScore(double averageRating, int completionRate, long noShowCount, long reviewCount) {
        if (reviewCount == 0 && completionRate == 0 && noShowCount == 0) {
            return 0;
        }

        int ratingScore = (int) Math.round(averageRating * 20);
        int completionScore = (int) Math.round(completionRate * 0.3);
        int reviewBonus = (int) Math.min(reviewCount * 2, 10);
        int noShowPenalty = (int) Math.min(noShowCount * 12, 36);
        int rawScore = ratingScore + completionScore + reviewBonus - noShowPenalty;
        return Math.max(0, Math.min(rawScore, 100));
    }

    private TrustLevel resolveTrustLevel(long reviewCount,
                                         long completedTradeCount,
                                         long noShowCount,
                                         double averageRating,
                                         int trustScore) {
        if (reviewCount == 0 && completedTradeCount == 0 && noShowCount == 0) {
            return TrustLevel.NEW;
        }
        if (trustScore >= 90 && averageRating >= 4.7 && noShowCount == 0) {
            return TrustLevel.TOP;
        }
        if (trustScore >= 75 && averageRating >= 4.2 && noShowCount <= 1) {
            return TrustLevel.GOOD;
        }
        if (trustScore < 55 || averageRating > 0 && averageRating < 3.5 || noShowCount >= 2) {
            return TrustLevel.CAUTION;
        }
        return TrustLevel.NORMAL;
    }
}
