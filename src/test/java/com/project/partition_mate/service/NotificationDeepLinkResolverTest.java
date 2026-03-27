package com.project.partition_mate.service;

import com.project.partition_mate.domain.UserNotificationType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationDeepLinkResolverTest {

    private final NotificationDeepLinkResolver notificationDeepLinkResolver = new NotificationDeepLinkResolver();

    @Test
    void 알림_타입별_딥링크를_반환한다() {
        // given
        Long partyId = 42L;

        // when
        String joinConfirmedLink = notificationDeepLinkResolver.resolve(UserNotificationType.PARTY_JOIN_CONFIRMED, partyId);
        String pickupUpdatedLink = notificationDeepLinkResolver.resolve(UserNotificationType.PICKUP_UPDATED, partyId);
        String partyUpdatedLink = notificationDeepLinkResolver.resolve(UserNotificationType.PARTY_UPDATED, partyId);
        String partyClosedLink = notificationDeepLinkResolver.resolve(UserNotificationType.PARTY_CLOSED, partyId);

        // then
        assertThat(joinConfirmedLink).isEqualTo("/chat/42");
        assertThat(pickupUpdatedLink).isEqualTo("/parties/42");
        assertThat(partyUpdatedLink).isEqualTo("/parties/42");
        assertThat(partyClosedLink).isEqualTo("/notifications");
    }

    @Test
    void 파티아이디가_없으면_알림내역으로_보낸다() {
        // given
        // when
        String link = notificationDeepLinkResolver.resolve(UserNotificationType.PARTY_CLOSED, null);

        // then
        assertThat(link).isEqualTo("/notifications");
    }
}
