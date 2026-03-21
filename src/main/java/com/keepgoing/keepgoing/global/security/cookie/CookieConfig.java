package com.keepgoing.keepgoing.global.security.cookie;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TokenCookieProperties.class)
public class CookieConfig {
}
