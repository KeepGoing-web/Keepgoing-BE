package com.keepgoing.keepgoing.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keepgoing.keepgoing.auth.controller.SignupRequest;
import com.keepgoing.keepgoing.auth.service.SignupCommand;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Auth 관련 테스트에서 재사용할 요청/JSON 생성 헬퍼
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuthTestFixtures {

    public static SignupRequest validSignupRequest() {
        return new SignupRequest(
                "user@example.com",
                "P@ssw0rd!",
                "홍길동"
        );
    }

    public static SignupRequest signupRequestWithEmail(String email) {
        SignupRequest base = validSignupRequest();
        return new SignupRequest(
                email,
                base.password(),
                base.name()
        );
    }

    public static SignupRequest signupRequestWithPassword(String password) {
        SignupRequest base = validSignupRequest();
        return new SignupRequest(
                base.email(),
                password,
                base.name()
        );
    }

    public static SignupCommand validSignupCommand() {
        return SignupCommand.builder()
                .email("user@example.com")
                .rawPassword("P@ssw0rd!")
                .name("홍길동")
                .build();
    }

    public static SignupCommand signupCommandWithEmail(String email) {
        return SignupCommand.builder()
                .email(email)
                .rawPassword("P@ssw0rd!")
                .name("홍길동")
                .build();
    }

    /**
     * JSON 직렬화 헬퍼
     */
    public static String toJson(ObjectMapper objectMapper, Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
