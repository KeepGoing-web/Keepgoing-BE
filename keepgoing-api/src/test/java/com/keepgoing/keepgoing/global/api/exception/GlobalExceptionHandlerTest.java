package com.keepgoing.keepgoing.global.api.exception;

import com.keepgoing.keepgoing.support.PostgreSqlTestContainerSupport;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

//@WebMvcTest(controllers = TestErrorController.class)
//@AutoConfigureMockMvc(addFilters = false)
//@Import(GlobalExceptionHandler.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest extends PostgreSqlTestContainerSupport {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("비즈니스 예외는 ErrorResponse를 반환해야 한다.")
    void business_exception_should_return_error_response() throws Exception {
        mockMvc.perform(get("/test/errors/business"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("사용자를 찾을 수 없습니다."))
                .andExpect(jsonPath("$.error.fieldErrors").doesNotExist());
    }

    @Test
    @DisplayName("검증 예외는 FieldErrors를 반환해야 한다.")
    void validation_exception_should_return_field_errors() throws Exception {
        var body = objectMapper.writeValueAsString(new TestErrorController.ValidationRequest());

        mockMvc.perform(post("/test/errors/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.fieldErrors", hasSize(1)))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("email"))
                .andExpect(jsonPath("$.error.fieldErrors[0].message").value("이메일은 필수입니다."));
    }

    @Test
    @DisplayName("예상치 못한 예외는 내부 서버 에러를 반환해야 한다.")
    void unexpected_exception_should_return_internal_server_error() throws Exception {
        mockMvc.perform(get("/test/errors/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"));
    }
}