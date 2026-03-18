package com.project.partition_mate.repository;

import com.project.partition_mate.domain.User;
import com.project.partition_mate.domain.WebPushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WebPushSubscriptionRepository extends JpaRepository<WebPushSubscription, Long> {

    Optional<WebPushSubscription> findByEndpoint(String endpoint);

    Optional<WebPushSubscription> findByIdAndUser(Long id, User user);

    List<WebPushSubscription> findAllByUserOrderByUpdatedAtDesc(User user);
}
