package com.project.partition_mate.service;

import com.project.partition_mate.domain.User;
import com.project.partition_mate.domain.UserNotificationPreference;
import com.project.partition_mate.domain.UserNotificationType;
import com.project.partition_mate.dto.NotificationPreferenceResponse;
import com.project.partition_mate.dto.UpdateNotificationPreferencesRequest;
import com.project.partition_mate.exception.BusinessException;
import com.project.partition_mate.repository.UserNotificationPreferenceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserNotificationPreferenceService {

    private final UserNotificationPreferenceRepository userNotificationPreferenceRepository;
    private final Clock clock;

    public List<NotificationPreferenceResponse> getPreferences(User user) {
        Map<UserNotificationType, UserNotificationPreference> savedPreferences = loadPreferences(user);

        return Arrays.stream(UserNotificationType.values())
                .map(type -> NotificationPreferenceResponse.of(type, resolveWebPushEnabled(type, savedPreferences.get(type))))
                .toList();
    }

    @Transactional
    public List<NotificationPreferenceResponse> updatePreferences(User user, UpdateNotificationPreferencesRequest request) {
        validateRequest(request);

        Map<UserNotificationType, UserNotificationPreference> savedPreferences = loadPreferences(user);
        LocalDateTime now = LocalDateTime.now(clock);

        for (UpdateNotificationPreferencesRequest.Preference preference : request.getPreferences()) {
            UserNotificationType type = preference.getType();
            if (preference.isWebPushEnabled() && !type.supportsWebPush()) {
                throw BusinessException.notificationTypeDoesNotSupportWebPush(type.getDisplayLabel());
            }

            UserNotificationPreference savedPreference = savedPreferences.get(type);
            if (savedPreference == null) {
                userNotificationPreferenceRepository.save(
                        UserNotificationPreference.create(user, type, preference.isWebPushEnabled(), now)
                );
                continue;
            }

            savedPreference.update(preference.isWebPushEnabled(), now);
        }

        return getPreferences(user);
    }

    public boolean isWebPushEnabled(User user, UserNotificationType notificationType) {
        if (!notificationType.supportsWebPush()) {
            return false;
        }

        return userNotificationPreferenceRepository.findByUserAndNotificationType(user, notificationType)
                .map(UserNotificationPreference::isWebPushEnabled)
                .orElse(notificationType.defaultWebPushEnabled());
    }

    private void validateRequest(UpdateNotificationPreferencesRequest request) {
        Set<UserNotificationType> duplicateCheck = new HashSet<>();
        for (UpdateNotificationPreferencesRequest.Preference preference : request.getPreferences()) {
            if (!duplicateCheck.add(preference.getType())) {
                throw BusinessException.duplicateNotificationPreferenceType();
            }
        }
    }

    private Map<UserNotificationType, UserNotificationPreference> loadPreferences(User user) {
        Map<UserNotificationType, UserNotificationPreference> preferences = new EnumMap<>(UserNotificationType.class);
        for (UserNotificationPreference preference : userNotificationPreferenceRepository.findAllByUser(user)) {
            preferences.put(preference.getNotificationType(), preference);
        }
        return preferences;
    }

    private boolean resolveWebPushEnabled(UserNotificationType type, UserNotificationPreference preference) {
        if (!type.supportsWebPush()) {
            return false;
        }

        if (preference == null) {
            return type.defaultWebPushEnabled();
        }

        return preference.isWebPushEnabled();
    }
}
