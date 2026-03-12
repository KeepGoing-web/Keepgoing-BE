package com.keepgoing.keepgoing.auth.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

	private final String redirectUri;

	public OAuth2AuthenticationFailureHandler(
			@Value("${oauth.redirect-uri}") String redirectUri) {
		this.redirectUri = redirectUri;
	}

	@Override
	public void onAuthenticationFailure(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException exception
	) throws IOException, ServletException {
		String errorCode = resolveErrorCode(exception);

		String uriString = UriComponentsBuilder
				.fromUriString(redirectUri)
				.queryParam("error", errorCode)
				.build()
				.toUriString();
		response.sendRedirect(uriString);
	}

	private String resolveErrorCode(AuthenticationException exception) {
		if (!(exception instanceof OAuth2AuthenticationException oauthException)) {
			return "oauth_login_failed";
		}

		return switch (oauthException.getError().getErrorCode()) {
			case OAuth2ErrorCodes.ACCESS_DENIED -> "oauth_access_denied";
			case OAuth2ErrorCodes.INVALID_REQUEST -> "oauth_invalid_user_info";
			default -> "oauth_login_failed";
		};
	}
}
