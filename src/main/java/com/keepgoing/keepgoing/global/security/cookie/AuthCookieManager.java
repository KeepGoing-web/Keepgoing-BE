package com.keepgoing.keepgoing.global.security.cookie;

import com.keepgoing.keepgoing.global.security.cookie.TokenCookieProperties.CookieSpec;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(TokenCookieProperties.class)
@RequiredArgsConstructor
public class AuthCookieManager {

	private final TokenCookieProperties tokenCookieProperties;

	public void addAccessToken(HttpServletResponse response, String token) {
		addCookie(response, tokenCookieProperties.accessToken(), token);
	}

	public void addRefreshToken(HttpServletResponse response, String token) {
		addCookie(response, tokenCookieProperties.refreshToken(), token);
	}

	public void clearAccessToken(HttpServletResponse response) {
		expireCookie(response, tokenCookieProperties.accessToken());
	}

	public void clearRefreshToken(HttpServletResponse response) {
		expireCookie(response, tokenCookieProperties.refreshToken());
	}

	public void clearAllTokens(HttpServletResponse response) {
		clearAccessToken(response);
		clearRefreshToken(response);
	}

	private void addCookie(
			HttpServletResponse response,
			CookieSpec spec,
			String value
	) {
		ResponseCookie cookie = ResponseCookie.from(spec.name(), value)
				.httpOnly(true)
				.secure(tokenCookieProperties.secure())
				.sameSite(tokenCookieProperties.sameSite())
				.path(spec.path())
				.maxAge(spec.maxAge())
				.build();

		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	private void expireCookie(
			HttpServletResponse response,
			CookieSpec spec
	) {
		ResponseCookie cookie = ResponseCookie.from(spec.name(), "")
				.httpOnly(true)
				.secure(tokenCookieProperties.secure())
				.sameSite(tokenCookieProperties.sameSite())
				.path(spec.path())
				.maxAge(0)
				.build();

		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}
}
