package com.keepgoing.keepgoing.global.config;

import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

	@Bean
	Jackson2ObjectMapperBuilderCustomizer jsonNullableCustomizer() {
		return builder -> builder.modules(new JsonNullableModule());
	}
}
