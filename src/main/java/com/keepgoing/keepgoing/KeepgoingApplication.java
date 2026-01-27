package com.keepgoing.keepgoing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
public class KeepgoingApplication {

    public static void main(String[] args) {
        SpringApplication.run(KeepgoingApplication.class, args);
    }

}
