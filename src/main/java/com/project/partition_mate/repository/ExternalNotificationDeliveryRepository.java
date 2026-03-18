package com.project.partition_mate.repository;

import com.project.partition_mate.domain.ExternalNotificationDelivery;
import com.project.partition_mate.domain.ExternalNotificationDeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ExternalNotificationDeliveryRepository extends JpaRepository<ExternalNotificationDelivery, Long> {

    boolean existsByDeliveryKey(String deliveryKey);

    List<ExternalNotificationDelivery> findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByIdAsc(
            ExternalNotificationDeliveryStatus status,
            LocalDateTime nextAttemptAt
    );
}
