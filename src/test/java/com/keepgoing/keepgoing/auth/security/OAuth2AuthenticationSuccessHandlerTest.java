package com.keepgoing.keepgoing.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.keepgoing.keepgoing.auth.OAuthTestFixtures.GoogleUserFixture;
import com.keepgoing.keepgoing.global.security.cookie.AuthCookieManager;
import com.keepgoing.keepgoing.global.security.jwt.JwtProvider;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

	private static final String OAUTH_CALLBACK = "http://localhost:5173/oauth/callback";

	@Mock
	JwtProvider jwtProvider;

	OAuth2AuthenticationSuccessHandler handler;

	@Mock
	AuthCookieManager authCookieManager;

	@BeforeEach
	void setup() {
		handler = new OAuth2AuthenticationSuccessHandler(
				OAUTH_CALLBACK,
				jwtProvider,
				authCookieManager
		);
	}

	@Test
	@DisplayName("인증 성공 시 인증 쿠키를 설정하고 콜백 URI로 redirect한다.")
	void success_redirectsWithTokens() throws Exception {
		// given
		DefaultOAuth2User delegate = GoogleUserFixture.builder()
				.name(null)
				.build()
				.oauth2User();
		CustomOAuth2User principal = new CustomOAuth2User(delegate, 1L, "ROLE_USER");
		Authentication authentication = new UsernamePasswordAuthenticationToken(
				principal,
				null,
				principal.getAuthorities()
		);
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		given(jwtProvider.generateAccessToken(1L, List.of("ROLE_USER")))
				.willReturn("access-token");
		given(jwtProvider.generateRefreshToken(1L))
				.willReturn("refresh-token");

		// when
		handler.onAuthenticationSuccess(request, response, authentication);

		// then
		assertThat(response.getRedirectedUrl()).isEqualTo(OAUTH_CALLBACK);

		then(jwtProvider).should().generateAccessToken(1L, List.of("ROLE_USER"));
		then(jwtProvider).should().generateRefreshToken(1L);
		then(authCookieManager).should().addAccessToken(response, "access-token");
		then(authCookieManager).should().addRefreshToken(response, "refresh-token");
	}
}