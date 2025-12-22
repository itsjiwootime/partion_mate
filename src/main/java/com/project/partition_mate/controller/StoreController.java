package com.project.partition_mate.controller;

import com.project.partition_mate.domain.Store;
import com.project.partition_mate.dto.PartyResponse;
import com.project.partition_mate.dto.StoreResponse;
import com.project.partition_mate.service.PartyService;
import com.project.partition_mate.service.StoreService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;
    private final PartyService partyService;


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

        List<PartyResponse> parties = storeService.findPartiesByStoreId(storeId);

        return ResponseEntity.ok(parties);
    }
}
