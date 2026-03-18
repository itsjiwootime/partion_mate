package com.project.partition_mate.controller;

import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.UpsertWebPushSubscriptionRequest;
import com.project.partition_mate.dto.WebPushSubscriptionResponse;
import com.project.partition_mate.dto.WebPushConfigurationResponse;
import com.project.partition_mate.security.CustomUserDetails;
import com.project.partition_mate.service.WebPushSubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/push-subscriptions")
@RequiredArgsConstructor
public class WebPushSubscriptionController {

    private final WebPushSubscriptionService webPushSubscriptionService;

    @PutMapping
    public ResponseEntity<WebPushSubscriptionResponse> upsert(@RequestBody @Valid UpsertWebPushSubscriptionRequest request) {
        return ResponseEntity.ok(webPushSubscriptionService.upsert(getCurrentUser(), request));
    }

    @GetMapping
    public ResponseEntity<List<WebPushSubscriptionResponse>> getMySubscriptions() {
        return ResponseEntity.ok(webPushSubscriptionService.getSubscriptions(getCurrentUser()));
    }

    @GetMapping("/config")
    public ResponseEntity<WebPushConfigurationResponse> getConfiguration() {
        return ResponseEntity.ok(webPushSubscriptionService.getConfiguration());
    }

    @DeleteMapping("/{subscriptionId}")
    public ResponseEntity<Void> delete(@PathVariable Long subscriptionId) {
        webPushSubscriptionService.delete(getCurrentUser(), subscriptionId);
        return ResponseEntity.noContent().build();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        return principal.getUser();
    }
}
