package com.keepgoing.keepgoing;

import com.keepgoing.keepgoing.global.security.cookie.TokenCookieProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableConfigurationProperties(TokenCookieProperties.class)
public class KeepgoingApplication {

    public static void main(String[] args) {
        SpringApplication.run(KeepgoingApplication.class, args);
    }

}
