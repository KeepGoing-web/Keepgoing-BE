package com.keepgoing.keepgoing.global.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cors")
public record CorsProperties(
        List<String> allowedOrigins,
        List<String> allowedMethods,
        Long maxAge
) {
    public CorsProperties {
        if (allowedMethods == null) {
            allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
        }

        if (maxAge == null) {
            maxAge = 3600L;
        }
    }
}
