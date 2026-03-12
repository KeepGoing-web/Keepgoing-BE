package com.keepgoing.keepgoing.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

class OAuth2AuthenticationFailureHandlerTest {

	private static final String OAUTH_CALLBACK = "http://localhost:5173/oauth/callback";

	OAuth2AuthenticationFailureHandler handler;

	@BeforeEach
	void setUp() {
		handler = new OAuth2AuthenticationFailureHandler(OAUTH_CALLBACK);
	}

	@Test
	@DisplayName("OAuth 접근 거부 시 프론트 엔드로 oauth_access_denied를 전달한다.")
	void failure_accessDenied_redirectsWithError() throws Exception {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		OAuth2AuthenticationException exception = new OAuth2AuthenticationException(
				new OAuth2Error(OAuth2ErrorCodes.ACCESS_DENIED),
				"access denied"
		);

		// when
		handler.onAuthenticationFailure(request, response, exception);

		// then
		String redirectedUrl = response.getRedirectedUrl();
		assertThat(redirectedUrl).isNotNull();

		UriComponents uri = UriComponentsBuilder.fromUriString(redirectedUrl).build();
		assertThat(uri.getScheme()).isEqualTo("http");
		assertThat(uri.getHost()).isEqualTo("localhost");
		assertThat(uri.getPort()).isEqualTo(5173);
		assertThat(uri.getPath()).isEqualTo("/oauth/callback");
		assertThat(uri.getQueryParams().getFirst("error")).isEqualTo("oauth_access_denied");
	}

	@Test
	@DisplayName("필수 사용자 정보 누락시 프론트엔드로 oauth_invalid_user_info를 전달한다")
	void failure_invalidRequest_redirectsWithError() throws Exception {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		OAuth2AuthenticationException exception = new OAuth2AuthenticationException(
				new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST),
				"invalid request"
		);

		// when
		handler.onAuthenticationFailure(request, response, exception);

		// then
		UriComponents uri = UriComponentsBuilder
				.fromUriString(Objects.requireNonNull(response.getRedirectedUrl()))
				.build();
		assertThat(uri.getQueryParams().getFirst("error")).isEqualTo("oauth_invalid_user_info");
	}

	@Test
	@DisplayName("알 수 없는 인증 실패 시 프론트앤드로 oauth_login_failed를 전달한다")
	void failure_unknown_redirectsWithDefaultError() throws Exception {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		AuthenticationExceptionStub exception = new AuthenticationExceptionStub("unknown");

		// when
		handler.onAuthenticationFailure(request, response, exception);

		// then

		UriComponents uri = UriComponentsBuilder.fromUriString(Objects.requireNonNull(response.getRedirectedUrl()))
				.build();
		assertThat(uri.getQueryParams().getFirst("error")).isEqualTo("oauth_login_failed");
	}

	private static class AuthenticationExceptionStub
			extends org.springframework.security.core.AuthenticationException {
		protected AuthenticationExceptionStub(String msg) {
			super(msg);
		}
	}
}