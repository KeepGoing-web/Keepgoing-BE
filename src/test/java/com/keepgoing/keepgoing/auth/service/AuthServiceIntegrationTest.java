package com.keepgoing.keepgoing.auth.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keepgoing.keepgoing.auth.AuthTestFixtures;
import com.keepgoing.keepgoing.auth.service.dto.LoginCommand;
import com.keepgoing.keepgoing.auth.service.dto.LoginResult;
import com.keepgoing.keepgoing.auth.service.dto.SignupCommand;
import com.keepgoing.keepgoing.auth.service.dto.SignupResult;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.domain.UserPasswordCredential;
import com.keepgoing.keepgoing.user.repository.UserPasswordCredentialRepository;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@Transactional
@Slf4j
class AuthServiceIntegrationTest {

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserPasswordCredentialRepository credentialRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    AuthService authService;

    @Nested
    @DisplayName("회원가입")
    class Signup {
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

    @Nested
    @DisplayName("로그인")
    class Login {

        private void signupTestUser() {
            authService.signup(AuthTestFixtures.validSignupCommand());
        }

        @Test
        @DisplayName("성공 시 accessToken, refreshToken, userId를 반환한다.")
        void login_success() {
            // given
            signupTestUser();
            LoginCommand command = AuthTestFixtures.validLoginCommand();

            // when
            LoginResult result = authService.login(command);

            // then
            assertThat(result.accessToken()).isNotBlank();
            assertThat(result.refreshToken()).isNotBlank();
            assertThat(result.userId()).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 로그인 시 BadCredentialsException이 발생한다.")
        void login_with_nonexistent_email_throws() {
            // given
            LoginCommand command = AuthTestFixtures.loginCommandWithEmail("nonExistent@examle.com");

            // when & then
            assertThatThrownBy(() -> authService.login(command))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("잘못된 비밀번호로 로그인 시 BadCredentialsExceptions이 발생한다.")
        void login_with_wrong_password_throws() {
            // given
            signupTestUser();
            LoginCommand command = AuthTestFixtures.loginCommandWithPassword("WrongP@ss1!");

            // when & then
            assertThatThrownBy(() -> authService.login(command))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("정지된 계정으로 로그인 시 LockedException이 발생한다.")
        void login_with_suspended_account_throws() {
            // given
            signupTestUser();

            // 계정 정지 처리
            User user = userRepository.findByEmail("user@example.com").orElseThrow();
            user.suspend();
            userRepository.saveAndFlush(user);  // 즉시 DB 반영

            LoginCommand command = AuthTestFixtures.validLoginCommand();

            // when & then
            assertThatThrownBy(() -> authService.login(command))
                    .isInstanceOf(LockedException.class);
        }
    }
}