package com.project.partition_mate.controller;

import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.MyJoinedPartyResponse;
import com.project.partition_mate.dto.NotificationPreferenceResponse;
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

    @GetMapping("/me/parties")
    public ResponseEntity<List<MyJoinedPartyResponse>> getMyParties() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        User user = principal.getUser();

        return ResponseEntity.ok(userService.getMyParties(user));
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
}
