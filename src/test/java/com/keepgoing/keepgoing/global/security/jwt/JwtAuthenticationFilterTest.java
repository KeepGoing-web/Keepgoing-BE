package com.keepgoing.keepgoing.global.security.jwt;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.keepgoing.keepgoing.global.security.cookie.TokenCookieProperties;
import com.keepgoing.keepgoing.global.security.cookie.TokenCookieProperties.CookieSpec;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

	private static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
	private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
	private static final String BEARER_PREFIX = "Bearer ";
	private static final String ACCESS_TOKEN_VALUE = "access-token";
	private static final String ROLE_USER = "ROLE_USER";
	private static final String COOKIE_TOKEN_VALUE = "cookie-token";
	private static final String HEADER_TOKEN = "header-token";
	private static final String AUTHORITY = "authority";

	@Mock
	JwtProvider jwtProvider;

	JwtAuthenticationFilter jwtAuthenticationFilter;
	MockHttpServletRequest request;
	MockHttpServletResponse response;
	MockFilterChain filterChain;

	@BeforeEach
	void setUp() {
		TokenCookieProperties tokenCookieProperties = new TokenCookieProperties(
				false,
				"Lax",
				new CookieSpec(ACCESS_TOKEN_COOKIE_NAME, "/api", 3600L),
				new CookieSpec(REFRESH_TOKEN_COOKIE_NAME, "/api/auth", 604800L)
		);
		jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtProvider, tokenCookieProperties);
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
		filterChain = new MockFilterChain();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("Authorization Bearer 헤더가 있으면 인증한다.")
	void authenticate_withBearerHeader() throws Exception {
		// given
		request.addHeader(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + ACCESS_TOKEN_VALUE);

		given(jwtProvider.getUserIdFromToken(ACCESS_TOKEN_VALUE)).willReturn(1L);
		given(jwtProvider.getRolesFromToken(ACCESS_TOKEN_VALUE)).willReturn(List.of(ROLE_USER));

		// when
		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		// then
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		assertThat(authentication).isNotNull();
		assertThat(authentication.getPrincipal()).isEqualTo(1L);
		assertThat(authentication.getAuthorities())
				.extracting(AUTHORITY)
				.containsExactly(ROLE_USER);

		then(jwtProvider).should().getUserIdFromToken(ACCESS_TOKEN_VALUE);
		then(jwtProvider).should().getRolesFromToken(ACCESS_TOKEN_VALUE);
	}

	@Test
	@DisplayName("Bearer 헤더가 없으면 access_token 쿠키로 인증한다")
	void authentication_withAccessTokenCookie() throws Exception {
		// given
		request.setCookies(new Cookie(ACCESS_TOKEN_COOKIE_NAME, COOKIE_TOKEN_VALUE));

		given(jwtProvider.getUserIdFromToken(COOKIE_TOKEN_VALUE)).willReturn(2L);
		given(jwtProvider.getRolesFromToken(COOKIE_TOKEN_VALUE)).willReturn(List.of(ROLE_USER));

		// when
		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		// then
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		assertThat(authentication).isNotNull();
		assertThat(authentication.getPrincipal()).isEqualTo(2L);
		assertThat(authentication.getAuthorities())
				.extracting(AUTHORITY)
				.containsExactly(ROLE_USER);
	}

	@Test
	@DisplayName("헤더와 쿠기가 함께 있으면 헤더를 우선한다")
	void authenticate_headerTakesPrecedenceOverCookie() throws Exception {
		// given
		request.addHeader(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + HEADER_TOKEN);
		request.setCookies(new Cookie(ACCESS_TOKEN_COOKIE_NAME, COOKIE_TOKEN_VALUE));

		given(jwtProvider.getUserIdFromToken(HEADER_TOKEN)).willReturn(3L);
		given(jwtProvider.getRolesFromToken(HEADER_TOKEN)).willReturn(List.of(ROLE_USER));

		// when
		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		// then
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		assertThat(authentication).isNotNull();
		assertThat(authentication.getPrincipal()).isEqualTo(3L);
		assertThat(authentication.getAuthorities())
				.extracting(AUTHORITY)
				.contains(ROLE_USER);

		then(jwtProvider).should().getUserIdFromToken(HEADER_TOKEN);
		then(jwtProvider).should().getRolesFromToken(HEADER_TOKEN);
		then(jwtProvider).should(never()).getUserIdFromToken(COOKIE_TOKEN_VALUE);
		then(jwtProvider).should(never()).getRolesFromToken(COOKIE_TOKEN_VALUE);
	}

	@Test
	@DisplayName("토큰이 없으면 인증하지 않는다")
	void doNotAuthenticate_whenNoTokenExists() throws Exception {
		// when
		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		// then
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	@DisplayName("토큰 검증 중 예외가 발생하면 인증하지 않고 다음 필터로 진행한다")
	void doNotAuthenticate_whenTokenValidationFails() throws Exception {
		// given
		request.addHeader(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + "invalid-token");

		given(jwtProvider.getUserIdFromToken("invalid-token"))
				.willThrow(new RuntimeException("invalid token"));

		// when
		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		// then
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		assertThat(filterChain.getResponse()).isSameAs(response);
		assertThat(filterChain.getRequest()).isSameAs(request);
	}

	@Test
	@DisplayName("Authorization 헤더가 Bearer 타입이 아니면 헤더를 무시하고 쿠키로 인증한다")
	void authenticate_withCookie_whenHeaderIsNotBearerType() throws Exception {
		// given
		request.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + ACCESS_TOKEN_VALUE);
		request.setCookies(new Cookie(ACCESS_TOKEN_COOKIE_NAME, COOKIE_TOKEN_VALUE));

		given(jwtProvider.getUserIdFromToken(COOKIE_TOKEN_VALUE)).willReturn(4L);
		given(jwtProvider.getRolesFromToken(COOKIE_TOKEN_VALUE)).willReturn(List.of(ROLE_USER));

		// when
		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		// then
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		assertThat(authentication).isNotNull();
		assertThat(authentication.getPrincipal()).isEqualTo(4L);

		then(jwtProvider).should(never()).getUserIdFromToken("Basic " + ACCESS_TOKEN_VALUE);
	}
}