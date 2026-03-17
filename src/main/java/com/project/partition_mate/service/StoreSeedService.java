package com.project.partition_mate.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.partition_mate.domain.Store;
import com.project.partition_mate.repository.StoreRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StoreSeedService {

    private final StoreRepository storeRepository;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Transactional
    public SeedResult synchronize(String resourceLocation) {
        return synchronize(resourceLoader.getResource(resourceLocation));
    }

    @Transactional
    public SeedResult synchronize(Resource resource) {
        List<StoreSeedItem> items = readSeedItems(resource);
        validateDuplicateNames(items);

        int createdCount = 0;
        int updatedCount = 0;

        for (StoreSeedItem item : items) {
            Store existingStore = storeRepository.findByName(item.name()).orElse(null);
            if (existingStore == null) {
                storeRepository.save(new Store(
                        item.name(),
                        item.address(),
                        item.openTime(),
                        item.closeTime(),
                        item.latitude(),
                        item.longitude(),
                        item.phone()
                ));
                createdCount++;
                continue;
            }

            if (existingStore.applySeed(
                    item.address(),
                    item.openTime(),
                    item.closeTime(),
                    item.latitude(),
                    item.longitude(),
                    item.phone()
            )) {
                updatedCount++;
            }
        }

        return new SeedResult(createdCount, updatedCount, items.size());
    }

    private List<StoreSeedItem> readSeedItems(Resource resource) {
        if (!resource.exists()) {
            throw new IllegalArgumentException("지점 시드 파일을 찾을 수 없습니다: " + resource.getDescription());
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<>() {
            });
        } catch (IOException exception) {
            throw new IllegalStateException("지점 시드 파일을 읽을 수 없습니다: " + resource.getDescription(), exception);
        }
    }

    private void validateDuplicateNames(List<StoreSeedItem> items) {
        Set<String> names = new HashSet<>();
        for (StoreSeedItem item : items) {
            if (!names.add(item.name())) {
                throw new IllegalArgumentException("지점 시드 파일에 중복된 지점명이 있습니다: " + item.name());
            }
        }
    }

    public record SeedResult(int createdCount, int updatedCount, int totalCount) {
    }

    private record StoreSeedItem(
            String name,
            String address,
            LocalTime openTime,
            LocalTime closeTime,
            String phone,
            Double latitude,
            Double longitude
    ) {
        private StoreSeedItem {
            Objects.requireNonNull(name);
            Objects.requireNonNull(address);
            Objects.requireNonNull(openTime);
            Objects.requireNonNull(closeTime);
            Objects.requireNonNull(phone);
            Objects.requireNonNull(latitude);
            Objects.requireNonNull(longitude);
        }
    }
}
