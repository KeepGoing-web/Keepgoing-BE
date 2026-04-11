package com.keepgoing.keepgoing.global.config;

import com.keepgoing.keepgoing.auth.security.CustomOAuth2UserService;
import com.keepgoing.keepgoing.auth.security.CustomOidcUserService;
import com.keepgoing.keepgoing.auth.security.OAuth2AuthenticationFailureHandler;
import com.keepgoing.keepgoing.auth.security.OAuth2AuthenticationSuccessHandler;
import com.keepgoing.keepgoing.global.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private static final String AUTH_API_PREFIX = "/api/auth";
	private static final String ACTUATOR_HEALTH_PATTERN = "/actuator/health/**";
	private static final String USERS_ME_PATH = "/api/users/me";
	private static final String USERS_ME_CHANGE_PASSWORD_PATH = "/api/users/me/change-password";
	private static final String FOLDERS_API_PATTERN = "/api/folders/**";
	private static final String NOTES_API_PATTERN = "/api/notes/**";
	private static final String NOTES_ME_API_PATTERN = "/api/notes/me/**";

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
	private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			CorsConfigurationSource corsConfigurationSource,
			CustomOAuth2UserService customOAuth2UserService,
			CustomOidcUserService customOidcUserService
	) throws Exception {

		http
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.csrf(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)   // 기본 /login 폼 비활성화
				.httpBasic(AbstractHttpConfigurer::disable) // 기본 Basic 인증 비활성화
				.sessionManagement(session ->
						session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(ex ->
						ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
				.authorizeHttpRequests(auth -> {
							auth
									// Swagger & OpenAPI 문서 경로 허용
									.requestMatchers(
											"/v3/api-docs/**",
											"/swagger-ui/**",
											"/swagger-ui.html",
											ACTUATOR_HEALTH_PATTERN
									).permitAll()
									// auth 도메인
									.requestMatchers(
											AUTH_API_PREFIX + "/signup",
											AUTH_API_PREFIX + "/login",
											AUTH_API_PREFIX + "/refresh",
											AUTH_API_PREFIX + "/logout"
									).permitAll()
									// user 도메인
									.requestMatchers(USERS_ME_PATH).authenticated()
									.requestMatchers(USERS_ME_CHANGE_PASSWORD_PATH).authenticated()
									// folder 도메인
									.requestMatchers(FOLDERS_API_PATTERN).authenticated()
									// note 도메인, 노트 조회는 공개, 쓰기(생성/수정/삭제)는 인증 필요
									.requestMatchers(NOTES_ME_API_PATTERN).authenticated()
									.requestMatchers(HttpMethod.GET, NOTES_API_PATTERN).permitAll()
									.requestMatchers(HttpMethod.POST, NOTES_API_PATTERN).authenticated()
									.requestMatchers(HttpMethod.PUT, NOTES_API_PATTERN).authenticated()
									.requestMatchers(HttpMethod.PATCH, NOTES_API_PATTERN).authenticated()
									.requestMatchers(HttpMethod.DELETE, NOTES_API_PATTERN).authenticated()
									// oauth
									.requestMatchers(
											"/oauth2/**",
											"/login/oauth2/**"
									).permitAll()
									// 나머지는 인증 필요
									.anyRequest().authenticated();
						}
				)
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.oauth2Login(oauth2 -> oauth2
						.userInfoEndpoint(userInfoEndpointConfig -> userInfoEndpointConfig
								.userService(customOAuth2UserService)
								.oidcUserService(customOidcUserService))
						.successHandler(oAuth2AuthenticationSuccessHandler)
						.failureHandler(oAuth2AuthenticationFailureHandler)
				);

		return http.build();
	}
}
