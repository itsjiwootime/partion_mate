package com.project.partition_mate.service;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyStatus;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.dto.PartyResponse;
import com.project.partition_mate.dto.StoreResponse;
import com.project.partition_mate.repository.PartyRepository;
import com.project.partition_mate.repository.StoreRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final PartyRepository partyRepository;

    public Store findById(Long id) {

        return storeRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Store with id " + id + " not found"));
    }

    public List<Store> findAll() {
        return storeRepository.findAll();
    }

    public List<PartyResponse> findPartiesByStoreId(Long storeId){

        List<Party> parties = partyRepository.findAllByStoreId(storeId);

        return parties.stream().map(PartyResponse::from).toList();

    }

    public List<StoreResponse> getNearbyStores(double lat, double lon) {
        List<Store> stores = storeRepository.findAll();

        return stores.stream()
                .map(store -> {
                    double distance = calculateDistance(store.getLatitude(), store.getLongitude(), lat, lon);
                    long partyCount = countRecruitingParties(store.getId());
                    return StoreResponse.of(
                            store,
                            distance,
                            partyCount
                    );
                })
                .sorted(Comparator.comparingDouble(StoreResponse::getDistance))
                .toList();
    }

    public long countRecruitingParties(Long storeId) {
        // 모집 중만 집계
        return partyRepository.countByStoreIdAndPartyStatus(storeId, PartyStatus.RECRUITING);
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // 지구 반지름 (km)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

}
