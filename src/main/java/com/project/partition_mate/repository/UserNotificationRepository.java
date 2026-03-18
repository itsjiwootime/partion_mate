package com.project.partition_mate.repository;

import com.project.partition_mate.domain.User;
import com.project.partition_mate.domain.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    List<UserNotification> findAllByUserOrderByCreatedAtDesc(User user);

    boolean existsByExternalKey(String externalKey);

    Optional<UserNotification> findByExternalKey(String externalKey);
}
