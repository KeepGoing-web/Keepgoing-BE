package com.keepgoing.keepgoing.auth.security;

import com.keepgoing.keepgoing.global.security.cookie.AuthCookieManager;
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

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final String redirectUri;
	private final JwtProvider jwtProvider;
	private final AuthCookieManager authCookieManager;

	public OAuth2AuthenticationSuccessHandler(
			@Value("${oauth.redirect-uri}") String redirectUri,
			JwtProvider jwtProvider,
			AuthCookieManager authCookieManager
	) {
		this.redirectUri = redirectUri;
		this.jwtProvider = jwtProvider;
		this.authCookieManager = authCookieManager;
	}

	@Override
	public void onAuthenticationSuccess(
			HttpServletRequest request,
			HttpServletResponse response,
			Authentication authentication
	) throws IOException, ServletException {
		CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();
		Long userId = principal.getUserId();
		String role = principal.getRole();

		String accessToken = jwtProvider.generateAccessToken(userId, List.of(role));
		String refreshToken = jwtProvider.generateRefreshToken(userId);

		authCookieManager.addAccessToken(response, accessToken);
		authCookieManager.addRefreshToken(response, refreshToken);
		response.sendRedirect(redirectUri);
	}
}
