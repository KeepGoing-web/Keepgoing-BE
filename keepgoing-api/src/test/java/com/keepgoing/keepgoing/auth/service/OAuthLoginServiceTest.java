package com.keepgoing.keepgoing.auth.service;

import com.keepgoing.keepgoing.support.PostgreSqlTestContainerSupport;

import static org.assertj.core.api.Assertions.assertThat;

import com.keepgoing.keepgoing.user.domain.OAuthProvider;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.domain.UserOAuthAccount;
import com.keepgoing.keepgoing.user.repository.UserOAuthAccountRepository;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@Transactional
class OAuthLoginServiceTest extends PostgreSqlTestContainerSupport {

    private static final OAuthProvider PROVIDER = OAuthProvider.GOOGLE;
    private static final String EMAIL = "hong@gmail.com";
    private static final String PROVIDER_USER_ID = "google-123";
    private static final String NAME = "홍길동";
    private static final String AVATAR_URL = "https://pic.google.com/old";

    @Autowired
    OAuthLoginService oAuthLoginService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserOAuthAccountRepository userOAuthAccountRepository;

    @Autowired
    EntityManager entityManager;

    @Nested
    @DisplayName("신규 사용자")
    class NewUser {

        @Test
        @DisplayName("User와 UserOAuthAccount가 생성된다.")
        void newUser_createUserAndUserOAuthAccount() {
            // given
            // when
            oAuthLoginService.processOAuthLogin(EMAIL, PROVIDER_USER_ID, AVATAR_URL, NAME);

            // then
            assertThat(userRepository.findByEmail(EMAIL))
                    .isPresent()
                    .get()
                    .satisfies(user ->
                            assertThat(user.getName()).isEqualTo(NAME));

            assertThat(userOAuthAccountRepository.findByProviderAndProviderUserId(PROVIDER, PROVIDER_USER_ID))
                    .isPresent()
                    .get()
                    .satisfies(userOAuthAccount -> {
                        assertThat(userOAuthAccount.getEmail()).isEqualTo(EMAIL);
                        assertThat(userOAuthAccount.getAvatarUrl()).isEqualTo(AVATAR_URL);
                    });
        }
    }

    @Nested
    @DisplayName("기존 이메일 사용자")
    class ExistingUser {

        @Test
        @DisplayName("기존 User에 OAuthAccount가 연동된다.")
        void existingUser_linksOAuthAccountToExistingUser() {
            // given
            User existingUser = User.create(EMAIL, NAME);
            userRepository.save(existingUser);

            // when
            oAuthLoginService.processOAuthLogin(EMAIL, PROVIDER_USER_ID, AVATAR_URL, NAME);

            // then
            assertThat(userOAuthAccountRepository.findByUserId(existingUser.getId()))
                    .hasSize(1)
                    .first()
                    .satisfies(userOAuthAccount -> {
                        assertThat(userOAuthAccount.getProviderUserId()).isEqualTo(PROVIDER_USER_ID);
                        assertThat(userOAuthAccount.getUser().getId()).isEqualTo(existingUser.getId());
                    });
            assertThat(userRepository.count()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("재로그인")
    class Relogin {

        @Test
        @DisplayName("프로필 정보가 갱신된다.")
        void relogin_updatesProfile() {
            // given
            User existingUser = User.create(EMAIL, NAME);
            userRepository.save(existingUser);
            UserOAuthAccount existingUserAccount = UserOAuthAccount.create(
                    existingUser,
                    PROVIDER,
                    PROVIDER_USER_ID,
                    EMAIL,
                    "https://old-picture"
            );
            userOAuthAccountRepository.save(existingUserAccount);
            entityManager.flush();
            entityManager.clear();

            // when
            oAuthLoginService.processOAuthLogin(EMAIL, PROVIDER_USER_ID, AVATAR_URL, NAME);
            entityManager.flush();
            entityManager.clear();

            // then
            assertThat(userRepository.count()).isEqualTo(1L);
            assertThat(userOAuthAccountRepository.count()).isEqualTo(1L);
            assertThat(userOAuthAccountRepository.findByUserId(existingUser.getId()))
                    .first()
                    .satisfies(userOAuthAccount ->
                            assertThat(userOAuthAccount.getAvatarUrl()).isEqualTo(AVATAR_URL));
        }
    }
}