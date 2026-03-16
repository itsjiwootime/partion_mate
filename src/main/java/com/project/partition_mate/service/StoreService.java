package com.project.partition_mate.service;

import com.project.partition_mate.domain.PartyStatus;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.dto.PartyResponse;
import com.project.partition_mate.dto.StoreResponse;
import com.project.partition_mate.repository.PartyRepository;
import com.project.partition_mate.repository.StoreRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreService {

    private static final double DEFAULT_SEARCH_RADIUS_KM = 50.0;
    private static final double LATITUDE_KM = 111.0;
    private static final double LONGITUDE_KM = 111.320;

    private final StoreRepository storeRepository;
    private final PartyRepository partyRepository;

    public Store findById(Long id) {

        return storeRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Store with id " + id + " not found"));
    }

    public List<Store> findAll() {
        return storeRepository.findAll();
    }

    @Cacheable(cacheNames = StoreQueryCacheSupport.STORE_PARTIES_CACHE, key = "#storeId")
    public List<PartyResponse> findPartiesByStoreId(Long storeId) {
        return partyRepository.findResponsesByStoreId(storeId);
    }

    @Cacheable(
            cacheNames = StoreQueryCacheSupport.NEARBY_STORES_CACHE,
            key = "T(com.project.partition_mate.service.StoreQueryCacheSupport).nearbyCacheKey(#lat, #lon)"
    )
    public List<StoreResponse> getNearbyStores(double lat, double lon) {
        SearchBounds bounds = SearchBounds.of(lat, lon, DEFAULT_SEARCH_RADIUS_KM);
        List<StoreResponse> boundedStores = storeRepository.findNearbyStoresWithinBounds(
                lat,
                lon,
                bounds.minLatitude(),
                bounds.maxLatitude(),
                bounds.minLongitude(),
                bounds.maxLongitude(),
                PartyStatus.RECRUITING.name()
        ).stream().map(this::toStoreResponse).toList();

        if (!boundedStores.isEmpty()) {
            return boundedStores;
        }

        return storeRepository.findAllOrderByDistance(lat, lon, PartyStatus.RECRUITING.name())
                .stream()
                .map(this::toStoreResponse)
                .toList();
    }

    public long countRecruitingParties(Long storeId) {
        return partyRepository.countByStoreIdAndPartyStatus(storeId, PartyStatus.RECRUITING);
    }

    private StoreResponse toStoreResponse(StoreRepository.NearbyStoreProjection projection) {
        return new StoreResponse(
                projection.getId(),
                projection.getName(),
                projection.getAddress(),
                projection.getOpenTime(),
                projection.getCloseTime(),
                projection.getPhone(),
                projection.getDistance(),
                projection.getPartyCount()
        );
    }

    private record SearchBounds(double minLatitude,
                                double maxLatitude,
                                double minLongitude,
                                double maxLongitude) {

        private static SearchBounds of(double latitude, double longitude, double radiusKm) {
            double latitudeDelta = radiusKm / LATITUDE_KM;
            double longitudeBase = LONGITUDE_KM * Math.max(Math.cos(Math.toRadians(latitude)), 0.1);
            double longitudeDelta = radiusKm / longitudeBase;

            return new SearchBounds(
                    latitude - latitudeDelta,
                    latitude + latitudeDelta,
                    longitude - longitudeDelta,
                    longitude + longitudeDelta
            );
        }
    }
}
