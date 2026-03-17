package com.project.partition_mate.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "auth.refresh")
public class RefreshTokenCookieProperties {

    private String cookieName = "pm_refresh_token";
    private boolean cookieSecure = false;
    private String cookieSameSite = "Lax";
}
