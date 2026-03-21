package com.keepgoing.keepgoing.global.security.cookie;

import static org.assertj.core.api.Assertions.assertThat;

import com.keepgoing.keepgoing.global.security.cookie.TokenCookieProperties.CookieSpec;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthCookieManagerTest {

	private AuthCookieManager authCookieManager;
	private MockHttpServletResponse response;

	@BeforeEach
	void setUp() {
		TokenCookieProperties prop = new TokenCookieProperties(
				false,
				"Lax",
				new CookieSpec("access_token", "/api", 3600L),
				new CookieSpec("refresh_token", "/api/auth", 604800L)
		);
		authCookieManager = new AuthCookieManager(prop);
		response = new MockHttpServletResponse();
	}

	@Test
	@DisplayName("access token 쿠키를 추가한다.")
	void addAccessToken() {
		// given
		String token = "access-token";

		// when
		authCookieManager.addAccessToken(response, token);
		List<String> cookies = response.getHeaders(HttpHeaders.SET_COOKIE);

		// then
		assertThat(cookies).hasSize(1);
		assertThat(cookies.getFirst()).contains("access_token=access-token");
		assertThat(cookies.getFirst()).contains("Path=/api");
		assertThat(cookies.getFirst()).contains("Max-Age=3600");
		assertThat(cookies.getFirst()).contains("HttpOnly");
		assertThat(cookies.getFirst()).contains("SameSite=Lax");
	}

	@Test
	@DisplayName("refresh token 쿠키를 추가한다.")
	void addRefreshToken() {
		// given
		String token = "refresh-token";

		// when
		authCookieManager.addRefreshToken(response, token);
		List<String> cookies = response.getHeaders(HttpHeaders.SET_COOKIE);

		// then
		assertThat(cookies).hasSize(1);
		assertThat(cookies.getFirst()).contains("refresh_token=refresh-token");
		assertThat(cookies.getFirst()).contains("Path=/api/auth");
		assertThat(cookies.getFirst()).contains("Max-Age=604800");
		assertThat(cookies.getFirst()).contains("HttpOnly");
		assertThat(cookies.getFirst()).contains("SameSite=Lax");
	}

	@Test
	@DisplayName("access token 쿠키를 만료시킨다")
	void clearAccessToken() {
		// when
		authCookieManager.clearAccessToken(response);
		List<String> cookies = response.getHeaders(HttpHeaders.SET_COOKIE);

		// then
		assertThat(cookies.getFirst()).contains("access_token=");
		assertThat(cookies.getFirst()).contains("Path=/api");
		assertThat(cookies.getFirst()).contains("Max-Age=0");
		assertThat(cookies.getFirst()).contains("HttpOnly");
	}

	@Test
	@DisplayName("refresh token 쿠키를 만료시킨다")
	void clearRefreshToken() {
		// when
		authCookieManager.clearRefreshToken(response);
		List<String> cookies = response.getHeaders(HttpHeaders.SET_COOKIE);

		// then
		assertThat(cookies.getFirst()).contains("refresh_token=");
		assertThat(cookies.getFirst()).contains("Path=/api/auth");
		assertThat(cookies.getFirst()).contains("Max-Age=0");
		assertThat(cookies.getFirst()).contains("HttpOnly");
	}

	@Test
	@DisplayName("모든 토큰 쿠키를 만료시킨다")
	void clearAllTokens() {
		// when
		authCookieManager.clearAllTokens(response);
		List<String> cookies = response.getHeaders(HttpHeaders.SET_COOKIE);

		// then
		assertThat(cookies.get(0)).contains("access_token=");
		assertThat(cookies.get(0)).contains("Max-Age=0");
		assertThat(cookies.get(1)).contains("refresh_token=");
		assertThat(cookies.get(1)).contains("Max-Age=0");
	}
}