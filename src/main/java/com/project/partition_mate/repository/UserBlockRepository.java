package com.project.partition_mate.repository;

import com.project.partition_mate.domain.User;
import com.project.partition_mate.domain.UserBlock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    boolean existsByBlockerAndBlocked(User blocker, User blocked);

    Optional<UserBlock> findByBlockerAndBlocked(User blocker, User blocked);

    @EntityGraph(attributePaths = {"blocked"})
    List<UserBlock> findAllByBlockerOrderByCreatedAtDesc(User blocker);

    @Query("""
            select count(ub) > 0
            from UserBlock ub
            where (ub.blocker.id = :firstUserId and ub.blocked.id = :secondUserId)
               or (ub.blocker.id = :secondUserId and ub.blocked.id = :firstUserId)
            """)
    boolean existsBlockingRelation(@Param("firstUserId") Long firstUserId,
                                   @Param("secondUserId") Long secondUserId);
}
