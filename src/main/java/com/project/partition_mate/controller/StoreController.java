package com.project.partition_mate.controller;

import com.project.partition_mate.domain.Store;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.PartyResponse;
import com.project.partition_mate.dto.StoreResponse;
import com.project.partition_mate.security.CustomUserDetails;
import com.project.partition_mate.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @GetMapping("/{id}")
    public ResponseEntity<StoreResponse> getStore(@PathVariable Long id) {

        Store store = storeService.findById(id);

        long partyCount = storeService.countRecruitingParties(id);
        StoreResponse response = StoreResponse.from(store, partyCount);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<StoreResponse>> getNearbyStore(@RequestParam Double latitude, @RequestParam Double longitude) {
        List<StoreResponse> stores = storeService.getNearbyStores(latitude, longitude);

        return ResponseEntity.ok(stores);
    }

    @GetMapping("/{storeId}/parties")
    public ResponseEntity<List<PartyResponse>> getStoreParties(@PathVariable Long storeId) {

        List<PartyResponse> parties = storeService.findPartiesByStoreId(storeId, resolveCurrentUserOrNull());

        return ResponseEntity.ok(parties);
    }

    private User resolveCurrentUserOrNull() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;
        if (!(principal instanceof CustomUserDetails customUserDetails)) {
            return null;
        }
        return customUserDetails.getUser();
    }
}
