package com.keepgoing.keepgoing.global.security.cookie;

import com.keepgoing.keepgoing.global.security.cookie.TokenCookieProperties.CookieSpec;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class AuthCookieManager {

	private final TokenCookieProperties properties;

	public void addAccessToken(HttpServletResponse response, String token) {
		addCookie(response, properties.accessToken(), token);
	}

	public void addRefreshToken(HttpServletResponse response, String token) {
		addCookie(response, properties.refreshToken(), token);
	}

	public void clearAccessToken(HttpServletResponse response) {
		expireCookie(response, properties.accessToken());
	}

	public void clearRefreshToken(HttpServletResponse response) {
		expireCookie(response, properties.refreshToken());
	}

	public void clearAllTokens(HttpServletResponse response) {
		clearAccessToken(response);
		clearRefreshToken(response);
	}

	public Optional<String> extractRefreshToken(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return Optional.empty();
		}

		for (Cookie cookie : cookies) {
			if (cookie.getName().equals(properties.refreshToken().name())
					&& StringUtils.hasText(cookie.getValue())) {
				return Optional.of(cookie.getValue());
			}
		}
		return Optional.empty();
	}


	private void addCookie(
			HttpServletResponse response,
			CookieSpec spec,
			String value
	) {
		ResponseCookie cookie = ResponseCookie.from(spec.name(), value)
				.httpOnly(true)
				.secure(properties.secure())
				.sameSite(properties.sameSite())
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
				.secure(properties.secure())
				.sameSite(properties.sameSite())
				.path(spec.path())
				.maxAge(0)
				.build();

		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}
}
