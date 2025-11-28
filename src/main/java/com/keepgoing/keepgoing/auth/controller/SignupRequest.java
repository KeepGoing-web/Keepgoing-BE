package com.keepgoing.keepgoing.auth.controller;

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

        // TODO: 추후 비밀번호 찾기에서 커스텀 어노테이션으로 리팩토링 예정.
        @NotBlank
        @Size(min = 8, max = 64)
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*]).+$",
                message = "비밀번호는 대소문자, 숫자, 특수문자를 각각 1자 이상 포함해야 합니다."
        )
        @Schema(description = "평문 비밀번호", example = "P@ssw0rd!")
        String password,

        @NotBlank
        @Size(min = 1, max = 50)
        @Schema(description = "사용자 이름", example = "홍길동")
        String name
) {
}
