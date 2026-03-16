package com.project.partition_mate.repository;

import com.project.partition_mate.domain.User;
import com.project.partition_mate.domain.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    List<UserNotification> findAllByUserOrderByCreatedAtDesc(User user);

    boolean existsByExternalKey(String externalKey);
}
