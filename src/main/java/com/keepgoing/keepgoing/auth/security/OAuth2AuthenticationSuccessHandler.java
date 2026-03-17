package com.keepgoing.keepgoing.auth.security;

import com.keepgoing.keepgoing.global.security.jwt.JwtProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final JwtProvider jwtProvider;
	private final String redirectUri;

	public OAuth2AuthenticationSuccessHandler(
			JwtProvider jwtProvider,
			@Value("${oauth.redirect-uri}") String redirectUri
	) {
		this.jwtProvider = jwtProvider;
		this.redirectUri = redirectUri;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
	                                    Authentication authentication) throws IOException, ServletException {
		CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();
		Long userId = principal.getUserId();
		String role = principal.getRole();

		String accessToken = jwtProvider.generateAccessToken(userId, List.of(role));
		String refreshToken = jwtProvider.generateRefreshToken(userId);

		String uriString = UriComponentsBuilder
				.fromUriString(redirectUri)
				.queryParam("token", accessToken)
				.queryParam("refresh", refreshToken)
				.build().toUriString();
		response.sendRedirect(uriString);
	}
}
