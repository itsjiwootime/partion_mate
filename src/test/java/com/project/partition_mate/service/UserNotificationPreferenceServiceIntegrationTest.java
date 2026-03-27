package com.project.partition_mate.service;

import com.project.partition_mate.domain.User;
import com.project.partition_mate.domain.UserNotificationPreference;
import com.project.partition_mate.domain.UserNotificationType;
import com.project.partition_mate.dto.NotificationPreferenceResponse;
import com.project.partition_mate.dto.UpdateNotificationPreferencesRequest;
import com.project.partition_mate.exception.BusinessException;
import com.project.partition_mate.repository.UserNotificationPreferenceRepository;
import com.project.partition_mate.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
@ActiveProfiles("test")
class UserNotificationPreferenceServiceIntegrationTest {

    @Autowired
    private UserNotificationPreferenceService userNotificationPreferenceService;

    @Autowired
    private UserNotificationPreferenceRepository userNotificationPreferenceRepository;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        userNotificationPreferenceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 새_사용자_설정을_조회하면_지원타입은_기본_web_push_on으로_반환한다() {
        // given
        User user = userRepository.saveAndFlush(new User("member", "member@test.com", "pw", "서울", 37.5, 127.0));

        // when
        List<NotificationPreferenceResponse> responses = userNotificationPreferenceService.getPreferences(user);

        // then
        assertThat(responses).hasSize(UserNotificationType.values().length);
        assertThat(findByType(responses, UserNotificationType.PICKUP_UPDATED).isWebPushSupported()).isTrue();
        assertThat(findByType(responses, UserNotificationType.PICKUP_UPDATED).isWebPushEnabled()).isTrue();
        assertThat(findByType(responses, UserNotificationType.PARTY_UPDATED).isWebPushSupported()).isFalse();
        assertThat(findByType(responses, UserNotificationType.PARTY_UPDATED).isWebPushEnabled()).isFalse();
        assertThat(findByType(responses, UserNotificationType.PARTY_CLOSED).getDeepLinkTargetLabel()).isEqualTo("알림 내역");
    }

    @Test
    void 웹푸시_설정을_저장한다() {
        // given
        User user = userRepository.saveAndFlush(new User("member", "member@test.com", "pw", "서울", 37.5, 127.0));
        UpdateNotificationPreferencesRequest request = request(
                preference(UserNotificationType.PICKUP_UPDATED, false),
                preference(UserNotificationType.PARTY_CLOSED, false)
        );

        // when
        List<NotificationPreferenceResponse> responses = userNotificationPreferenceService.updatePreferences(user, request);

        // then
        assertThat(userNotificationPreferenceRepository.findAll()).hasSize(2)
                .extracting(UserNotificationPreference::getNotificationType)
                .containsExactlyInAnyOrder(UserNotificationType.PICKUP_UPDATED, UserNotificationType.PARTY_CLOSED);
        assertThat(findByType(responses, UserNotificationType.PICKUP_UPDATED).isWebPushEnabled()).isFalse();
        assertThat(findByType(responses, UserNotificationType.PARTY_CLOSED).isWebPushEnabled()).isFalse();
        assertThat(userNotificationPreferenceService.isWebPushEnabled(user, UserNotificationType.PICKUP_UPDATED)).isFalse();
    }

    @Test
    void 지원하지_않는_타입을_웹푸시_on으로_저장하면_예외가_발생한다() {
        // given
        User user = userRepository.saveAndFlush(new User("member", "member@test.com", "pw", "서울", 37.5, 127.0));
        UpdateNotificationPreferencesRequest request = request(
                preference(UserNotificationType.PARTY_UPDATED, true)
        );

        // when
        Throwable thrown = catchThrowable(() -> userNotificationPreferenceService.updatePreferences(user, request));

        // then
        assertThat(thrown)
                .isInstanceOf(BusinessException.class)
                .hasMessage("파티 조건 변경 알림은 아직 브라우저 푸시를 지원하지 않습니다.");
    }

    private NotificationPreferenceResponse findByType(List<NotificationPreferenceResponse> responses,
                                                      UserNotificationType type) {
        return responses.stream()
                .filter(response -> response.getType() == type)
                .findFirst()
                .orElseThrow();
    }

    private UpdateNotificationPreferencesRequest request(UpdateNotificationPreferencesRequest.Preference... preferences) {
        UpdateNotificationPreferencesRequest request = new UpdateNotificationPreferencesRequest();
        ReflectionTestUtils.setField(request, "preferences", List.of(preferences));
        return request;
    }

    private UpdateNotificationPreferencesRequest.Preference preference(UserNotificationType type, boolean webPushEnabled) {
        UpdateNotificationPreferencesRequest.Preference preference = new UpdateNotificationPreferencesRequest.Preference();
        ReflectionTestUtils.setField(preference, "type", type);
        ReflectionTestUtils.setField(preference, "webPushEnabled", webPushEnabled);
        return preference;
    }
}
