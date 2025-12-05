package com.keepgoing.keepgoing.auth.controller.dto;

import com.keepgoing.keepgoing.auth.validation.Password;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Schema(description = "회원가입 요청")
@Builder
public record SignupRequest(

        @Email
        @NotBlank
        @Schema(description = "로그인 이메일", example = "user@example.com")
        String email,

        @Password
        @Schema(description = "평문 비밀번호", example = "P@ssw0rd!")
        String password,

        @NotBlank
        @Size(min = 1, max = 50)
        @Schema(description = "사용자 이름", example = "홍길동")
        String name
) {
}
