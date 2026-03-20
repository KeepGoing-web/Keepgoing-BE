package com.keepgoing.keepgoing.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.spy;

import com.keepgoing.keepgoing.auth.service.OAuthLoginService;
import com.keepgoing.keepgoing.auth.service.dto.OAuthUserPrincipal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

	private static final String EMAIL = "user@gmail.com";
	private static final String USER_ID = "google-123";
	private static final String PICTURE = "https://image.test/profile.png";
	private static final String NAME = "홍길동";

	@Mock
	OAuthLoginService oAuthLoginService;

	@Mock
	OAuth2UserRequest userRequest;

	CustomOAuth2UserService service;

	@BeforeEach
	void setUp() {
		service = spy(new CustomOAuth2UserService(oAuthLoginService));
	}

	@Test
	@DisplayName("정상 Google 사용자 정보면 OAuthLoginService를 호출하고 CustomOAuth2User를 반환한다.")
	void success_loadUser() {
		// given
		DefaultOAuth2User delegate = new DefaultOAuth2User(
				List.of(new SimpleGrantedAuthority("ROLE_USER")),
				Map.of(
						"sub", USER_ID,
						"email", EMAIL,
						"name", NAME,
						"picture", PICTURE),
				"sub"
		);
		willReturn(delegate).given(service).loadProviderUser(userRequest);
		given(oAuthLoginService.processOAuthLogin(
				EMAIL,
				USER_ID,
				PICTURE,
				NAME
		)).willReturn(OAuthUserPrincipal.builder()
				.userId(1L)
				.role("ROLE_USER")
				.build());

		// when
		OAuth2User result = service.loadUser(userRequest);

		// then
		assertThat(result).isInstanceOf(CustomOAuth2User.class);

		CustomOAuth2User principal = (CustomOAuth2User) result;
		assertThat(principal.getUserId()).isEqualTo(1L);
		assertThat(principal.getRole()).isEqualTo("ROLE_USER");
		assertThat(principal.getAttributes().get("email")).isEqualTo(EMAIL);

		then(oAuthLoginService).should().processOAuthLogin(
				EMAIL,
				USER_ID,
				PICTURE,
				NAME
		);
	}

	@Test
	@DisplayName("필수 정보가 누락되면 OAuth2AuthenticationException이 발생하고 로그인 서비스는 호출되지 않는다.")
	void loadUser_missingRequiredField_throws() {
		// given
		DefaultOAuth2User delegate = new DefaultOAuth2User(
				List.of(new SimpleGrantedAuthority("ROLE_USER")),
				Map.of(
						"sub", USER_ID),
				"sub"
		);
		willReturn(delegate).given(service).loadProviderUser(userRequest);

		// when & then
		assertThatThrownBy(() -> service.loadUser(userRequest))
				.isInstanceOf(OAuth2AuthenticationException.class);

		then(oAuthLoginService).shouldHaveNoInteractions();
	}
}