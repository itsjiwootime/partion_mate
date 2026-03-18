package com.project.partition_mate.repository;

import com.project.partition_mate.domain.User;
import com.project.partition_mate.domain.UserNotificationPreference;
import com.project.partition_mate.domain.UserNotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserNotificationPreferenceRepository extends JpaRepository<UserNotificationPreference, Long> {

    List<UserNotificationPreference> findAllByUser(User user);

    Optional<UserNotificationPreference> findByUserAndNotificationType(User user, UserNotificationType notificationType);
}
