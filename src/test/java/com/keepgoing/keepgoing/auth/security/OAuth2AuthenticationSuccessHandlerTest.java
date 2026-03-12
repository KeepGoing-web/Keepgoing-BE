package com.keepgoing.keepgoing.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.keepgoing.keepgoing.global.security.jwt.JwtProvider;
import java.util.List;
import java.util.Map;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

	private static final String OAUTH_CALLBACK = "http://localhost:5173/oauth/callback";

	@Mock
	JwtProvider jwtProvider;

	OAuth2AuthenticationSuccessHandler handler;

	@BeforeEach
	void setup() {
		handler = new OAuth2AuthenticationSuccessHandler(
				jwtProvider,
				OAUTH_CALLBACK
		);
	}

	@Test
	@DisplayName("인증 성공 시 access token, refresh token을 포함한 redirect를 수행한다.")
	void success_redirectsWithTokens() throws Exception {
		// given
		DefaultOAuth2User delegate = new DefaultOAuth2User(
				List.of(new SimpleGrantedAuthority("ROLE_USER")),
				Map.of("sub", "google-123", "email", "user@gmail.com"),
				"sub"
		);
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
		String redirectedUrl = response.getRedirectedUrl();
		assertThat(redirectedUrl).isNotNull();

		UriComponents uri = UriComponentsBuilder.fromUriString(redirectedUrl).build();
		assertThat(uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort() + uri.getPath())
				.isEqualTo(OAUTH_CALLBACK);

		assertThat(uri.getQueryParams().getFirst("token"))
				.isEqualTo("access-token");
		assertThat(uri.getQueryParams().getFirst("refresh"))
				.isEqualTo("refresh-token");

		then(jwtProvider).should().generateAccessToken(1L, List.of("ROLE_USER"));
		then(jwtProvider).should().generateRefreshToken(1L);
	}
}