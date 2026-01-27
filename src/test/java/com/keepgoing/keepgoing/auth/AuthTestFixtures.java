package com.keepgoing.keepgoing.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keepgoing.keepgoing.auth.controller.dto.LoginRequest;
import com.keepgoing.keepgoing.auth.controller.dto.SignupRequest;
import com.keepgoing.keepgoing.auth.service.dto.LoginCommand;
import com.keepgoing.keepgoing.auth.service.dto.SignupCommand;
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

    public static LoginRequest validLoginRequest() {
        return new LoginRequest(
                "user@example.com",
                "P@ssw0rd!"
        );
    }

    public static LoginRequest loginRequestWithEmail(String email) {
        return new LoginRequest(
                email,
                "P@ssw0rd!"
        );
    }

    public static LoginRequest loginRequestWithPassword(String password) {
        return new LoginRequest(
                "user@example.com",
                password
        );
    }

    public static LoginCommand validLoginCommand() {
        return new LoginCommand(
                "user@example.com",
                "P@ssw0rd!"
        );
    }

    public static LoginCommand loginCommandWithEmail(String email) {
        return new LoginCommand(
                email,
                "P@ssw0rd!"
        );
    }

    public static LoginCommand loginCommandWithPassword(String password) {
        return new LoginCommand(
                "user@example.com",
                password
        );
    }
}
