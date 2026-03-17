package com.project.partition_mate.repository;

import com.project.partition_mate.domain.Report;
import com.project.partition_mate.domain.ReportReasonType;
import com.project.partition_mate.domain.ReportStatus;
import com.project.partition_mate.domain.ReportTargetType;
import com.project.partition_mate.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    @Query("""
            select count(r) > 0
            from Report r
            where r.reporter = :reporter
              and r.targetType = :targetType
              and r.reasonType = :reasonType
              and r.status = :status
              and ((:partyId is null and r.party is null) or r.party.id = :partyId)
              and ((:targetUserId is null and r.targetUser is null) or r.targetUser.id = :targetUserId)
            """)
    boolean existsDuplicate(@Param("reporter") User reporter,
                            @Param("targetType") ReportTargetType targetType,
                            @Param("partyId") Long partyId,
                            @Param("targetUserId") Long targetUserId,
                            @Param("reasonType") ReportReasonType reasonType,
                            @Param("status") ReportStatus status);

    @EntityGraph(attributePaths = {"party", "targetUser"})
    List<Report> findAllByReporterOrderByCreatedAtDesc(User reporter);
}
