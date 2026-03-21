package com.keepgoing.keepgoing.auth.security;

import static com.keepgoing.keepgoing.auth.OAuthTestFixtures.EMAIL;
import static com.keepgoing.keepgoing.auth.OAuthTestFixtures.NAME;
import static com.keepgoing.keepgoing.auth.OAuthTestFixtures.PICTURE;
import static com.keepgoing.keepgoing.auth.OAuthTestFixtures.USER_ID;
import static com.keepgoing.keepgoing.auth.OAuthTestFixtures.googleUserBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keepgoing.keepgoing.auth.OAuthTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.user.OAuth2User;

class GoogleOAuthAttributesTest {

	@Test
	@DisplayName("Google 사용자 정보가 정상이면 정상 추출한다.")
	void from_success() {
		// given
		OAuth2User user = OAuthTestFixtures.googleUser();

		// when
		GoogleOAuthAttributes attr = GoogleOAuthAttributes.from(user);

		// then
		assertThat(attr.email()).isEqualTo(EMAIL);
		assertThat(attr.providerUserId()).isEqualTo(USER_ID);
		assertThat(attr.avatarUrl()).isEqualTo(PICTURE);
		assertThat(attr.name()).isEqualTo(NAME);
	}

	@Test
	@DisplayName("name이 없으면 이메일 prefix를 name으로 사용한다")
	void from_withoutName_usesEmailPrefix() {
		// given
		OAuth2User user = OAuthTestFixtures.googleUserBuilder()
				.name(null)
				.build()
				.oauth2User();

		// when
		GoogleOAuthAttributes attr = GoogleOAuthAttributes.from(user);

		// then
		assertThat(attr.email()).isEqualTo(EMAIL);
		assertThat(attr.providerUserId()).isEqualTo(USER_ID);
		assertThat(attr.avatarUrl()).isEqualTo(PICTURE);
		assertThat(attr.name()).isEqualTo("user");
	}

	@Test
	@DisplayName("email이 없으면 OAuth2AuthenticationException이 발생한다")
	void from_withoutEmail_throws() {
		// given
		OAuth2User user = googleUserBuilder()
				.email(null)
				.build()
				.oauth2User();

		// when & then
		assertThatThrownBy(() -> GoogleOAuthAttributes.from(user))
				.isInstanceOf(OAuth2AuthenticationException.class)
				.satisfies(ex -> {
					OAuth2AuthenticationException oauthEx = (OAuth2AuthenticationException) ex;
					assertThat(oauthEx.getError().getErrorCode()).isEqualTo(OAuth2ErrorCodes.INVALID_REQUEST);
				});
	}

	@Test
	@DisplayName("sub가 없으면 OAuth2AuthenticationException이 발생한다")
	void from_withoutSub_throws() {
		// given
		OAuth2User user = OAuthTestFixtures.googleUserBuilder()
				.sub(null)
				.build()
				.oauth2User();

		// when & sub
		assertThatThrownBy(() -> GoogleOAuthAttributes.from(user))
				.isInstanceOf(OAuth2AuthenticationException.class)
				.satisfies(ex -> {
					OAuth2AuthenticationException oauthEx = (OAuth2AuthenticationException) ex;
					assertThat(oauthEx.getError().getErrorCode()).isEqualTo(OAuth2ErrorCodes.INVALID_REQUEST);
				});
	}
}