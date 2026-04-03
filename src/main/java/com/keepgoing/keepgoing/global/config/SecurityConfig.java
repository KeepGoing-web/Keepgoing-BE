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
				.authorizeHttpRequests(auth -> auth
						// Swagger & OpenAPI 문서 경로 허용
						.requestMatchers(
								"/v3/api-docs/**",
								"/swagger-ui/**",
								"/swagger-ui.html"
						).permitAll()
						// 회원가입/로그인 API 허용
						.requestMatchers(
								AUTH_API_PREFIX + "/signup",
								AUTH_API_PREFIX + "/login",
								AUTH_API_PREFIX + "/refresh",
								AUTH_API_PREFIX + "/logout"
						).permitAll()
                        .requestMatchers(
                                "/oauth2/**",
                                "/login/oauth2/**"
                        ).permitAll()
						.requestMatchers(
								"/api/posts/me/**",
								"/api/folders"
						).authenticated()
                        // 게시글 조회는 공개(개발/일반 사용자 접근), 쓰기(생성/수정/삭제)는 인증 필요
						// TODO: post를 note로 리팩토링
                        .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/posts/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/posts/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/posts/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/posts/**").authenticated()
						// 나머지는 인증 필요
						.anyRequest().authenticated()
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
