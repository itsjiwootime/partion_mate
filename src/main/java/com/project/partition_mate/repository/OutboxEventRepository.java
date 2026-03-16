package com.project.partition_mate.repository;

import com.project.partition_mate.domain.OutboxEvent;
import com.project.partition_mate.domain.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByIdAsc(OutboxEventStatus status, LocalDateTime nextAttemptAt);
}
