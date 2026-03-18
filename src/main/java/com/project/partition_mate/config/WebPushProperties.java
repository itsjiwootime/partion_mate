package com.project.partition_mate.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.web-push")
public class WebPushProperties {

    private boolean enabled;
    private String subject;
    private String publicKey;
    private String privateKey;
    private int ttlSeconds = 300;

    public boolean hasCredentials() {
        return publicKey != null
                && !publicKey.isBlank()
                && privateKey != null
                && !privateKey.isBlank()
                && subject != null
                && !subject.isBlank();
    }
}
