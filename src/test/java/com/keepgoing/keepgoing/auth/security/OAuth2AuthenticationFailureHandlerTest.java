package com.keepgoing.keepgoing.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class OAuth2AuthenticationFailureHandlerTest {

	private static final String OAUTH_CALLBACK = "http://localhost:5173/oauth/callback";

	OAuth2AuthenticationFailureHandler handler;

	@BeforeEach
	void setUp() {
		handler = new OAuth2AuthenticationFailureHandler(OAUTH_CALLBACK);
	}

	@Test
	@DisplayName("OAuth 인증 실패 시 프론트 콜백 URI로 redirect한다.")
	void failure_redirectsCallback() throws Exception {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		AuthenticationExceptionStub exception = new AuthenticationExceptionStub("fail");

		// when
		handler.onAuthenticationFailure(request, response, exception);

		// then
		assertThat(response.getRedirectedUrl()).isEqualTo(OAUTH_CALLBACK);
	}

	private static class AuthenticationExceptionStub
			extends org.springframework.security.core.AuthenticationException {
		protected AuthenticationExceptionStub(String msg) {
			super(msg);
		}
	}
}