package com.project.partition_mate.controller;

import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.CreateUserBlockRequest;
import com.project.partition_mate.dto.UserBlockResponse;
import com.project.partition_mate.security.CustomUserDetails;
import com.project.partition_mate.service.UserBlockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/blocks")
@RequiredArgsConstructor
public class UserBlockController {

    private final UserBlockService userBlockService;

    @PostMapping
    public ResponseEntity<UserBlockResponse> blockUser(@RequestBody @Valid CreateUserBlockRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                userBlockService.blockUser(getCurrentUser(), request.getTargetUserId())
        );
    }

    @DeleteMapping("/{targetUserId}")
    public ResponseEntity<Void> unblockUser(@PathVariable Long targetUserId) {
        userBlockService.unblockUser(getCurrentUser(), targetUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<UserBlockResponse>> getBlockedUsers() {
        return ResponseEntity.ok(userBlockService.getBlockedUsers(getCurrentUser()));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        return principal.getUser();
    }
}
