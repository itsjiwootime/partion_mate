package com.project.partition_mate.controller;

import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.MyJoinedPartyResponse;
import com.project.partition_mate.dto.NotificationPreferenceResponse;
import com.project.partition_mate.dto.PartyResponse;
import com.project.partition_mate.dto.SettlementSettingsResponse;
import com.project.partition_mate.dto.UpdateUserProfileRequest;
import com.project.partition_mate.dto.UpdateSettlementSettingsRequest;
import com.project.partition_mate.dto.UpdateNotificationPreferencesRequest;
import com.project.partition_mate.dto.UserNotificationResponse;
import com.project.partition_mate.dto.UserResponse;
import com.project.partition_mate.security.CustomUserDetails;
import com.project.partition_mate.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        User user = principal.getUser();

        UserResponse response = userService.getProfile(user);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(
            @RequestBody @Valid UpdateUserProfileRequest request
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        User user = principal.getUser();

        return ResponseEntity.ok(userService.updateProfile(user, request));
    }

    @GetMapping("/me/parties")
    public ResponseEntity<List<MyJoinedPartyResponse>> getMyParties() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        User user = principal.getUser();

        return ResponseEntity.ok(userService.getMyParties(user));
    }

    @GetMapping("/me/favorite-parties")
    public ResponseEntity<List<PartyResponse>> getMyFavoriteParties() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        User user = principal.getUser();

        return ResponseEntity.ok(userService.getFavoriteParties(user));
    }

    @PutMapping("/me/favorite-parties/{partyId}")
    public ResponseEntity<Void> saveFavoriteParty(@PathVariable Long partyId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        User user = principal.getUser();

        userService.saveFavoriteParty(user, partyId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me/favorite-parties/{partyId}")
    public ResponseEntity<Void> removeFavoriteParty(@PathVariable Long partyId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        User user = principal.getUser();

        userService.removeFavoriteParty(user, partyId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/notifications")
    public ResponseEntity<List<UserNotificationResponse>> getMyNotifications() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        User user = principal.getUser();

        return ResponseEntity.ok(userService.getNotifications(user));
    }

    @GetMapping("/me/notification-preferences")
    public ResponseEntity<List<NotificationPreferenceResponse>> getMyNotificationPreferences() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        User user = principal.getUser();

        return ResponseEntity.ok(userService.getNotificationPreferences(user));
    }

    @PutMapping("/me/notification-preferences")
    public ResponseEntity<List<NotificationPreferenceResponse>> updateMyNotificationPreferences(
            @RequestBody @Valid UpdateNotificationPreferencesRequest request
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        User user = principal.getUser();

        return ResponseEntity.ok(userService.updateNotificationPreferences(user, request));
    }

    @GetMapping("/me/settlement-settings")
    public ResponseEntity<SettlementSettingsResponse> getMySettlementSettings() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        User user = principal.getUser();

        return ResponseEntity.ok(userService.getSettlementSettings(user));
    }

    @PutMapping("/me/settlement-settings")
    public ResponseEntity<SettlementSettingsResponse> updateMySettlementSettings(
            @RequestBody @Valid UpdateSettlementSettingsRequest request
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        User user = principal.getUser();

        return ResponseEntity.ok(userService.updateSettlementSettings(user, request));
    }
}
