package com.keepgoing.keepgoing.auth.controller;


import static org.assertj.core.api.Assertions.assertThat;

import com.keepgoing.keepgoing.auth.AuthTestFixtures;
import com.keepgoing.keepgoing.auth.controller.dto.SignupRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class SignupRequestPasswordValidationTest {

    // 실제 Validator 생성
    private static final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @ParameterizedTest(name = "[{index}] 유효한 비밀번호: {0}")
    @MethodSource("validPasswords")
    @DisplayName("비밀번호가 규칙을 만족하면 검증 에러가 없어야 한다")
    void valid_password(String password) {
        // given
        SignupRequest dto = AuthTestFixtures.signupRequestWithPassword(password);

        // when
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(dto);

        // then
        assertThat(violations)
                .as("password=%s 일 때 violations", password)
                .isEmpty();
    }

    static Stream<String> validPasswords() {
        return Stream.of(
                "Aa1!aaaa",         // 최소 길이 + 각 조건 1개
                "P@ssw0rd!",        // 흔한 패턴
                "StrongP@ssw0rd1!", // 길고 복잡한 비밀번호
                "Abcdef1!@",        // 여러 특수문자
                "AAaa1!aa",         // 대문자 여러 개
                "Aa1234!a",         // 숫자 여러 개
                "Aa1!@#$%"          // 특수문자 여러 개
        );
    }

    @ParameterizedTest(name = "[{index}] 잘못된 비밀번호: {0}")
    @MethodSource("invalidPasswords")
    @DisplayName("비밀번호가 규칙을 어기면 검증 에러가 발생해야 한다")
    void invalid_passwords(String password) {
        // given
        SignupRequest dto = AuthTestFixtures.signupRequestWithPassword(password);

        // when
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(dto);

        // then
        assertThat(violations)
                .as("password=%s 일 때 violations", password)
                .isNotEmpty();

    }

    static Stream<String> invalidPasswords() {
        return Stream.of(
                // 길이 부족
                "Aa1!a",        // 5자
                "Aa1!aaa",      // 7자

                // 조합 규칙 위반
                "aa1!aaaa",     // 대문자 없음
                "AA1!AAAA",     // 소문자 없음
                "Aa!aaaaa",     // 숫자 없음
                "Aa1aaaaa",     // 특수문자 없음

                // 공백 관련
                "",             // 빈 문자열
                "        "      // 공백만
        );
    }
}
