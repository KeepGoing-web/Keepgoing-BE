package com.keepgoing.keepgoing.auth.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keepgoing.keepgoing.auth.AuthTestFixtures;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.user.domain.UserPasswordCredential;
import com.keepgoing.keepgoing.user.repository.UserPasswordCredentialRepository;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@Transactional
class AuthServiceIntegrationTest {

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserPasswordCredentialRepository credentialRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    AuthService authService;

    @Test
    @DisplayName("회원가입 성공 시 User/PasswordCredential을 저장하고 SignupResult를 반환한다.")
    void signup_success() {
        // given
        SignupCommand command = AuthTestFixtures.validSignupCommand();

        // when
        SignupResult result = authService.signup(command);

        // then
        assertThat(userRepository.existsByEmail(command.email())).isTrue();
        assertThat(result.id()).isNotNull();
        assertThat(result.email()).isEqualTo(command.email());
        assertThat(result.name()).isEqualTo(command.name());
    }

    @Test
    @DisplayName("회원가입 성공 시 Credential이 User와 연관되어 저장된다.")
    void signup_saves_credential_with_user_association() {
        // given
        SignupCommand command = AuthTestFixtures.validSignupCommand();

        // when
        SignupResult result = authService.signup(command);

        // then
        UserPasswordCredential credential = credentialRepository.findById(result.id()).orElseThrow();

        assertThat(credential.getUserId()).isEqualTo(result.id());
        assertThat(credential.getUser().getId()).isEqualTo(result.id());
        assertThat(credential.getLoginId()).isEqualTo(command.email());
    }

    @Test
    @DisplayName("비밀번호는 암호화되어 저장된다.")
    void password_is_encrypted() {
        // given
        SignupCommand command = AuthTestFixtures.validSignupCommand();
        String rawPassword = command.rawPassword();

        // when
        SignupResult result = authService.signup(command);

        // then
        UserPasswordCredential credential = credentialRepository.findById(result.id())
                .orElseThrow();

        assertThat(credential.getPasswordHash()).isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, credential.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("중복 이메일 가입 시 예외발생하고 아무것도 저장하지 않는다.")
    void duplicate_email_throws_and_rolls_back() {
        // given
        SignupCommand firstCommand = AuthTestFixtures.validSignupCommand();
        authService.signup(firstCommand);

        long userCountBefore = userRepository.count();

        // when & then
        SignupCommand duplicate = SignupCommand.builder()
                .email(firstCommand.email())
                .name("다른이름")
                .rawPassword("testtEst8!")
                .build();

        assertThatThrownBy(() -> authService.signup(duplicate))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException bex = (BusinessException) ex;
                    assertThat(bex.getErrorCode()).isEqualTo(ErrorCode.USER_ALREADY_EXISTS);
                });

        assertThat(userRepository.count()).isEqualTo(userCountBefore);
    }
}