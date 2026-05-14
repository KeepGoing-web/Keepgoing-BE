package com.keepgoing.keepgoing.global.security.cookie;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.cookie")
public record TokenCookieProperties(
		boolean secure,
		String sameSite,
		CookieSpec accessToken,
		CookieSpec refreshToken
) {
	public record CookieSpec(
			String name,
			String path,
			Long maxAge
	) {
		public CookieSpec {
			if (name == null || name.isBlank()) {
				throw new IllegalArgumentException("cookie name must not be blank");
			}
			if (path == null || path.isBlank()) {
				path = "/";
			}
			if (maxAge == null || maxAge <= 0) {
				throw new IllegalArgumentException("cookie maxAge must be positive");
			}
		}
	}
}
