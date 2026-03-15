package com.project.partition_mate.repository;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.domain.WaitingQueueEntry;
import com.project.partition_mate.domain.WaitingQueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WaitingQueueRepository extends JpaRepository<WaitingQueueEntry, Long> {

    boolean existsByPartyAndUserAndStatus(Party party, User user, WaitingQueueStatus status);

    List<WaitingQueueEntry> findAllByPartyAndStatusOrderByQueuedAtAsc(Party party, WaitingQueueStatus status);

    List<WaitingQueueEntry> findAllByUserAndStatusOrderByQueuedAtDesc(User user, WaitingQueueStatus status);

    Optional<WaitingQueueEntry> findFirstByPartyAndStatusOrderByQueuedAtAsc(Party party, WaitingQueueStatus status);

    Optional<WaitingQueueEntry> findFirstByPartyAndUserAndStatus(Party party, User user, WaitingQueueStatus status);
}
