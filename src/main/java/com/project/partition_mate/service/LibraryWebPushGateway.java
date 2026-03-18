package com.project.partition_mate.service;

import com.project.partition_mate.config.WebPushProperties;
import com.project.partition_mate.domain.WebPushSubscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Urgency;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

import java.security.Security;

@Slf4j
@Service
@RequiredArgsConstructor
public class LibraryWebPushGateway implements WebPushGateway {

    private final WebPushProperties webPushProperties;

    @Override
    public DeliveryResult send(WebPushSubscription subscription, String payloadJson) throws Exception {
        if (!webPushProperties.isEnabled() || !webPushProperties.hasCredentials()) {
            return new DeliveryResult(0);
        }

        registerBouncyCastleProviderIfNeeded();

        Notification notification = Notification.builder()
                .endpoint(subscription.getEndpoint())
                .userPublicKey(subscription.getP256dh())
                .userAuth(subscription.getAuth())
                .payload(payloadJson)
                .ttl(webPushProperties.getTtlSeconds())
                .urgency(Urgency.HIGH)
                .build();

        PushService pushService = new PushService(
                webPushProperties.getPublicKey(),
                webPushProperties.getPrivateKey(),
                webPushProperties.getSubject()
        );
        HttpResponse response = pushService.send(notification);

        return new DeliveryResult(response.getStatusLine().getStatusCode());
    }

    private void registerBouncyCastleProviderIfNeeded() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
            log.info("Registered BouncyCastle provider for Web Push delivery.");
        }
    }
}
