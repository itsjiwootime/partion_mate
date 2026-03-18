package com.project.partition_mate.service;

import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.UpsertWebPushSubscriptionRequest;
import com.project.partition_mate.dto.WebPushSubscriptionResponse;
import com.project.partition_mate.exception.BusinessException;
import com.project.partition_mate.repository.UserRepository;
import com.project.partition_mate.repository.WebPushSubscriptionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
@ActiveProfiles("test")
@TestExecutionListeners(
        listeners = {
                DependencyInjectionTestExecutionListener.class,
                DirtiesContextBeforeModesTestExecutionListener.class,
                DirtiesContextTestExecutionListener.class
        },
        mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS
)
class WebPushSubscriptionServiceIntegrationTest {

    @Autowired
    private WebPushSubscriptionService webPushSubscriptionService;

    @Autowired
    private WebPushSubscriptionRepository webPushSubscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        webPushSubscriptionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 같은_endpoint로_구독을_다시_저장하면_기존_행을_갱신한다() {
        // given
        User user = userRepository.saveAndFlush(createUser("member"));
        UpsertWebPushSubscriptionRequest firstRequest = request(
                "https://push.example.com/subscriptions/1",
                "old-p256dh",
                "old-auth",
                "Chrome/1.0"
        );
        WebPushSubscriptionResponse firstResponse = webPushSubscriptionService.upsert(user, firstRequest);

        UpsertWebPushSubscriptionRequest secondRequest = request(
                "https://push.example.com/subscriptions/1",
                "new-p256dh",
                "new-auth",
                "Chrome/2.0"
        );

        // when
        WebPushSubscriptionResponse secondResponse = webPushSubscriptionService.upsert(user, secondRequest);

        // then
        assertThat(secondResponse.getId()).isEqualTo(firstResponse.getId());
        assertThat(webPushSubscriptionRepository.findAll()).hasSize(1);
        assertThat(webPushSubscriptionRepository.findAll().getFirst().getP256dh()).isEqualTo("new-p256dh");
        assertThat(webPushSubscriptionRepository.findAll().getFirst().getAuth()).isEqualTo("new-auth");
    }

    @Test
    void 내_구독을_삭제하면_목록에서_사라진다() {
        // given
        User user = userRepository.saveAndFlush(createUser("member"));
        WebPushSubscriptionResponse response = webPushSubscriptionService.upsert(
                user,
                request("https://push.example.com/subscriptions/2", "key", "auth", "Edge/1.0")
        );

        // when
        webPushSubscriptionService.delete(user, response.getId());

        // then
        assertThat(webPushSubscriptionService.getSubscriptions(user)).isEmpty();
        assertThat(webPushSubscriptionRepository.findAll()).isEmpty();
    }

    @Test
    void 다른_사용자_구독을_삭제하면_예외가_발생한다() {
        // given
        User owner = userRepository.saveAndFlush(createUser("owner"));
        User other = userRepository.saveAndFlush(createUser("other"));
        WebPushSubscriptionResponse response = webPushSubscriptionService.upsert(
                owner,
                request("https://push.example.com/subscriptions/3", "key", "auth", "Safari/1.0")
        );

        // when
        Throwable thrown = catchThrowable(() -> webPushSubscriptionService.delete(other, response.getId()));

        // then
        assertThat(thrown)
                .isInstanceOf(BusinessException.class)
                .hasMessage("웹 푸시 구독 내역이 없습니다.");
    }

    private User createUser(String username) {
        return new User(username, username + "@test.com", "pw", "서울", 37.5, 127.0);
    }

    private UpsertWebPushSubscriptionRequest request(String endpoint,
                                                     String p256dh,
                                                     String auth,
                                                     String userAgent) {
        UpsertWebPushSubscriptionRequest request = new UpsertWebPushSubscriptionRequest();
        UpsertWebPushSubscriptionRequest.Keys keys = new UpsertWebPushSubscriptionRequest.Keys();
        ReflectionTestUtils.setField(request, "endpoint", endpoint);
        ReflectionTestUtils.setField(request, "userAgent", userAgent);
        ReflectionTestUtils.setField(keys, "p256dh", p256dh);
        ReflectionTestUtils.setField(keys, "auth", auth);
        ReflectionTestUtils.setField(request, "keys", keys);
        return request;
    }
}
