package com.project.partition_mate.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.seed.stores")
public class StoreSeedProperties {

    private boolean enabled = true;

    private String resource = "classpath:seeds/stores.json";
}
