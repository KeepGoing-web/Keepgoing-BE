package com.keepgoing.keepgoing.auth.security;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

public record GoogleOAuthAttributes(
		String email,
		String providerUserId,
		String avatarUrl,
		String name
) {
	public static GoogleOAuthAttributes from(OAuth2User oAuth2User) {
		String providerUserId = oAuth2User.getAttribute("sub");
		String email = oAuth2User.getAttribute("email");
		if (providerUserId == null || email == null) {
			throw new OAuth2AuthenticationException(
					new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST),
					"Google 필수 정보 누락"
			);
		}
		String name = oAuth2User.getAttribute("name");
		if (name == null) {
			name = email.split("@")[0];
		}
		String avatarUrl = oAuth2User.getAttribute("picture");

		return new GoogleOAuthAttributes(email, providerUserId, avatarUrl, name);
	}

	public static GoogleOAuthAttributes from(OidcUser oidcUser) {

		String providerUserId = oidcUser.getSubject();
		String email = oidcUser.getEmail();
		if (providerUserId == null || email == null) {
			throw new OAuth2AuthenticationException(
					new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST),
					"Google 필수 정보 누락"
			);
		}
		String name = oidcUser.getFullName();
		if (name == null) {
			name = email.split("@")[0];
		}
		String avatarUrl = oidcUser.getPicture();

		return new GoogleOAuthAttributes(email, providerUserId, avatarUrl, name);
	}
}

