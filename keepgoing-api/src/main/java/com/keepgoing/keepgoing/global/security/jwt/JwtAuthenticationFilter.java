package com.keepgoing.keepgoing.global.security.jwt;

import com.keepgoing.keepgoing.global.security.cookie.TokenCookieProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtProvider jwtProvider;
	private final TokenCookieProperties tokenCookieProperties;

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		String token = extractToken(request);

		if (token != null) {
			try {
				Long userId = jwtProvider.getUserIdFromToken(token);
				List<String> roles = jwtProvider.getRolesFromToken(token);

				List<SimpleGrantedAuthority> authorities = roles.stream()
						.map(SimpleGrantedAuthority::new)
						.toList();
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
						userId, null, authorities);
				SecurityContextHolder.getContext().setAuthentication(authentication);
			} catch (Exception e) {
				log.debug("JWT 인증 실패: {}", e.getMessage());
			}
		}
		filterChain.doFilter(request, response);
	}

	private String extractToken(HttpServletRequest request) {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);

		if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
			return header.substring(BEARER_PREFIX.length());
		}

		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}

		String accessTokenCookieName = tokenCookieProperties.accessToken().name();
		for (Cookie cookie : cookies) {
			if (accessTokenCookieName.equals(cookie.getName())
					&& StringUtils.hasText(cookie.getValue())) {
				return cookie.getValue();
			}
		}

		return null;
	}
}
