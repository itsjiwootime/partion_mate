package com.project.partition_mate.config;

import com.project.partition_mate.service.StoreSeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.seed.stores", name = "enabled", havingValue = "true")
public class StoreSeedInitializer implements ApplicationRunner {

    private final StoreSeedService storeSeedService;
    private final StoreSeedProperties storeSeedProperties;

    @Override
    public void run(ApplicationArguments args) {
        StoreSeedService.SeedResult result = storeSeedService.synchronize(storeSeedProperties.getResource());
        log.info("지점 시드 적재 완료 - 총 {}건, 생성 {}건, 업데이트 {}건",
                result.totalCount(),
                result.createdCount(),
                result.updatedCount());
    }
}
