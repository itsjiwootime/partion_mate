package com.project.partition_mate.repository;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.Review;
import com.project.partition_mate.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByPartyAndReviewerAndReviewee(Party party, User reviewer, User reviewee);

    long countByReviewee(User reviewee);

    @EntityGraph(attributePaths = {"party", "reviewer", "reviewee"})
    List<Review> findTop5ByRevieweeOrderByCreatedAtDesc(User reviewee);

    @EntityGraph(attributePaths = {"party", "reviewer", "reviewee"})
    List<Review> findAllByRevieweeOrderByCreatedAtDesc(User reviewee);

    @EntityGraph(attributePaths = {"party", "reviewer", "reviewee"})
    List<Review> findAllByPartyAndReviewer(Party party, User reviewer);

    @Query("select coalesce(avg(r.rating), 0) from Review r where r.reviewee = :reviewee")
    Double findAverageRatingByReviewee(@Param("reviewee") User reviewee);
}
