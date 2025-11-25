package com.keepgoing.keepgoing.global.config;

import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenApiCustomizer globalResponsesCustomizer() {
        return openApi -> openApi.getPaths().values().forEach(pathItem -> {
            pathItem.readOperations().forEach(operation -> {
                ApiResponses responses = operation.getResponses();

                // 500 공통
                responses.addApiResponse("500", new ApiResponse()
                        .description("서버 내부 오류")
                        .content(new Content()
                                .addMediaType("application/json", new MediaType()
                                        .schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))
                                        .example(Map.of(
                                                        "success", false,
                                                        "error", Map.of(
                                                                "code", ErrorCode.INTERNAL_SERVER_ERROR.name(),
                                                                "message", ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage()
                                                        )
                                                )
                                        )
                                )
                        ));
            });
        });
    }
}