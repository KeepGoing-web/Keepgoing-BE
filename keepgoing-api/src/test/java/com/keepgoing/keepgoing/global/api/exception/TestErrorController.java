package com.keepgoing.keepgoing.global.api.exception;

import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test/errors")
public class TestErrorController {

    @GetMapping("/business")
    void businessError() {
        // service
        throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }

    @GetMapping("/unexpected")
    void unexpectedError() {
        throw new RuntimeException("boom");
    }

    @PostMapping("/validation")
    void validationError(@RequestBody @Valid ValidationRequest request) {
    }

    static class ValidationRequest {

        @NotBlank(message = "이메일은 필수입니다.")
        public String email;
    }
}
