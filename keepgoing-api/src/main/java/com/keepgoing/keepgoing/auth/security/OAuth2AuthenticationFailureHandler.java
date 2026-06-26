package com.keepgoing.keepgoing.auth.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
@Slf4j
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
		log.warn("OAuth2 login failed. type={}, message={}, redirectUri={}",
				exception.getClass().getName(),
				exception.getMessage(),
				redirectUri,
				exception);
		response.sendRedirect(redirectUri);
	}
}
