package com.project.partition_mate.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "review",
        uniqueConstraints = @UniqueConstraint(columnNames = {"party_id", "reviewer_id", "reviewee_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewee_id", nullable = false)
    private User reviewee;

    @Column(nullable = false)
    private Integer rating;

    @Column(length = 1000)
    private String comment;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private Review(Party party,
                   User reviewer,
                   User reviewee,
                   Integer rating,
                   String comment,
                   LocalDateTime createdAt) {
        this.party = Objects.requireNonNull(party, "파티는 필수입니다.");
        this.reviewer = Objects.requireNonNull(reviewer, "작성자는 필수입니다.");
        this.reviewee = Objects.requireNonNull(reviewee, "대상 사용자는 필수입니다.");
        validateRating(rating);
        this.rating = rating;
        this.comment = comment;
        this.createdAt = Objects.requireNonNull(createdAt, "생성 시각은 필수입니다.");

        if (reviewer.equals(reviewee)) {
            throw new IllegalArgumentException("자기 자신에게 후기를 작성할 수 없습니다.");
        }
    }

    public static Review create(Party party,
                                User reviewer,
                                User reviewee,
                                Integer rating,
                                String comment,
                                LocalDateTime createdAt) {
        return new Review(party, reviewer, reviewee, rating, comment, createdAt);
    }

    private void validateRating(Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("평점은 1점 이상 5점 이하여야 합니다.");
        }
    }
}
