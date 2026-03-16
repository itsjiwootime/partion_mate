package com.project.partition_mate.dto;

import com.project.partition_mate.domain.Review;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ReviewResponse {

    private final Long reviewId;
    private final Long partyId;
    private final String partyTitle;
    private final Long reviewerId;
    private final String reviewerName;
    private final Long revieweeId;
    private final Integer rating;
    private final String comment;
    private final LocalDateTime createdAt;
    private final String createdAtLabel;

    private ReviewResponse(Long reviewId,
                           Long partyId,
                           String partyTitle,
                           Long reviewerId,
                           String reviewerName,
                           Long revieweeId,
                           Integer rating,
                           String comment,
                           LocalDateTime createdAt) {
        this.reviewId = reviewId;
        this.partyId = partyId;
        this.partyTitle = partyTitle;
        this.reviewerId = reviewerId;
        this.reviewerName = reviewerName;
        this.revieweeId = revieweeId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
        this.createdAtLabel = DateTimeLabelFormatter.format(createdAt);
    }

    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getParty().getId(),
                review.getParty().getTitle(),
                review.getReviewer().getId(),
                review.getReviewer().getUsername(),
                review.getReviewee().getId(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
