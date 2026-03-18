package com.project.partition_mate.service;

import com.project.partition_mate.domain.User;
import com.project.partition_mate.domain.WebPushSubscription;
import com.project.partition_mate.dto.UpsertWebPushSubscriptionRequest;
import com.project.partition_mate.dto.WebPushSubscriptionResponse;
import com.project.partition_mate.exception.BusinessException;
import com.project.partition_mate.repository.WebPushSubscriptionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WebPushSubscriptionService {

    private final WebPushSubscriptionRepository webPushSubscriptionRepository;
    private final Clock clock;

    @Transactional
    public WebPushSubscriptionResponse upsert(User user, UpsertWebPushSubscriptionRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        WebPushSubscription subscription = webPushSubscriptionRepository.findByEndpoint(request.getEndpoint())
                .orElseGet(() -> WebPushSubscription.create(
                        user,
                        request.getEndpoint(),
                        request.getKeys().getP256dh(),
                        request.getKeys().getAuth(),
                        request.getUserAgent(),
                        now
                ));

        if (subscription.getId() != null) {
            subscription.refresh(
                    user,
                    request.getKeys().getP256dh(),
                    request.getKeys().getAuth(),
                    request.getUserAgent(),
                    now
            );
        }

        WebPushSubscription savedSubscription = webPushSubscriptionRepository.save(subscription);
        return WebPushSubscriptionResponse.from(savedSubscription);
    }

    public List<WebPushSubscriptionResponse> getSubscriptions(User user) {
        return webPushSubscriptionRepository.findAllByUserOrderByUpdatedAtDesc(user).stream()
                .map(WebPushSubscriptionResponse::from)
                .toList();
    }

    @Transactional
    public void delete(User user, Long subscriptionId) {
        WebPushSubscription subscription = webPushSubscriptionRepository.findByIdAndUser(subscriptionId, user)
                .orElseThrow(BusinessException::webPushSubscriptionNotFound);
        webPushSubscriptionRepository.delete(subscription);
    }
}
