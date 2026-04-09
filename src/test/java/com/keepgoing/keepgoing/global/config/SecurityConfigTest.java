package com.keepgoing.keepgoing.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.keepgoing.keepgoing.auth.security.CustomOAuth2UserService;
import com.keepgoing.keepgoing.auth.security.CustomOidcUserService;
import com.keepgoing.keepgoing.auth.security.OAuth2AuthenticationFailureHandler;
import com.keepgoing.keepgoing.auth.security.OAuth2AuthenticationSuccessHandler;
import com.keepgoing.keepgoing.global.security.cookie.TokenCookieProperties;
import com.keepgoing.keepgoing.global.security.jwt.JwtAuthenticationFilter;
import com.keepgoing.keepgoing.global.security.jwt.JwtProvider;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@WebMvcTest(controllers = SecurityConfigTest.ProbeController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, SecurityConfigTest.TestSupportConfig.class})
class SecurityConfigTest {

	private static final Long USER_ID = 1L;
	private static final String JSON = "application/json";
	private static final String EMPTY_JSON = "{}";
	private static final String VALID_NOTE_CREATE_JSON = """
			{
			  "title": "제목",
			  "content": "내용",
			  "visibility": "PRIVATE",
			  "aiCollectable": true
			}
			""";

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	JwtProvider jwtProvider;

	@MockitoBean
	TokenCookieProperties tokenCookieProperties;

	@MockitoBean
	CustomOAuth2UserService customOAuth2UserService;

	@MockitoBean
	CustomOidcUserService customOidcUserService;

	@MockitoBean
	OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

	@MockitoBean
	OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

	@MockitoBean
	ClientRegistrationRepository clientRegistrationRepository;

	@MockitoBean
	OAuth2AuthorizedClientRepository oAuth2AuthorizedClientRepository;

	@BeforeEach
	void setUp() {
		given(tokenCookieProperties.accessToken())
				.willReturn(new TokenCookieProperties.CookieSpec("access_token", "/", 3600L));
	}

	@Nested
	@DisplayName("Auth 정책")
	class AuthPolicy {

		@Test
		@DisplayName("POST /api/auth/signup, POST /api/auth/login 은 익명 접근을 허용한다")
		void signupAndLogin_allowAnonymous() throws Exception {
			assertNotBlocked(post("/api/auth/signup")
					.contentType(JSON)
					.content(EMPTY_JSON));

			assertNotBlocked(post("/api/auth/login")
					.contentType(JSON)
					.content(EMPTY_JSON));
		}

		@Test
		@DisplayName("POST /api/auth/refresh, POST /api/auth/logout 은 익명 접근을 허용한다")
		void refreshAndLogout_allowAnonymous() throws Exception {
			assertNotBlocked(post("/api/auth/refresh"));
			assertNotBlocked(post("/api/auth/logout"));
		}
	}

	@Nested
	@DisplayName("User 정책")
	class UserPolicy {

		@Test
		@DisplayName("GET /api/users/me 는 익명 사용자에게 401을 반환한다")
		void getMyInfo_requireAuthentication() throws Exception {
			assertUnauthorized(get("/api/users/me"));
		}

		@Test
		@DisplayName("GET /api/users/me 는 인증 사용자에게 열려 있다")
		void getMyInfo_allowAuthenticatedUser() throws Exception {
			assertNotBlocked(get("/api/users/me").with(authentication(authenticatedUser())));
		}

		@Test
		@DisplayName("PATCH /api/users/me 는 익명 사용자에게 401을 반환한다")
		void updateMyInfo_requireAuthentication() throws Exception {
			assertUnauthorized(patch("/api/users/me")
					.contentType(JSON)
					.content("{\"name\":\"새 이름\"}"));
		}

		@Test
		@DisplayName("PATCH /api/users/me 는 인증 사용자에게 열려 있다")
		void updateMyInfo_allowAuthenticatedUser() throws Exception {
			assertNotBlocked(patch("/api/users/me")
					.with(authentication(authenticatedUser()))
					.contentType(JSON)
					.content("{\"name\":\"새 이름\"}"));
		}

		@Test
		@DisplayName("POST /api/users/me/change-password 는 익명 사용자에게 401을 반환한다")
		void changePassword_requireAuthentication() throws Exception {
			assertUnauthorized(post("/api/users/me/change-password")
					.contentType(JSON)
					.content("""
							{
							  "currentPassword": "OldP@ssw0rd!",
							  "newPassword": "NewP@ssw0rd!"
							}
							"""));
		}

		@Test
		@DisplayName("POST /api/users/me/change-password 는 인증 사용자에게 열려 있다")
		void changePassword_allowAuthenticatedUser() throws Exception {
			assertNotBlocked(post("/api/users/me/change-password")
					.with(authentication(authenticatedUser()))
					.contentType(JSON)
					.content("""
							{
							  "currentPassword": "OldP@ssw0rd!",
							  "newPassword": "NewP@ssw0rd!"
							}
							"""));
		}
	}

	@Nested
	@DisplayName("Folder 정책")
	class FolderPolicy {

		@Test
		@DisplayName("GET /api/folders, POST /api/folders 는 익명 사용자에게 401을 반환한다")
		void folders_requireAuthentication() throws Exception {
			assertUnauthorized(get("/api/folders"));
			assertUnauthorized(post("/api/folders")
					.contentType(JSON)
					.content("{\"name\":\"backend\"}"));
		}

		@Test
		@DisplayName("GET /api/folders/tree, PATCH /api/folders/{folderId} 는 익명 사용자에게 401을 반환한다")
		void folderSubpaths_requireAuthentication() throws Exception {
			assertUnauthorized(get("/api/folders/tree"));
			assertUnauthorized(patch("/api/folders/{folderId}", 1L)
					.contentType(JSON)
					.content("{\"name\":\"backend\"}"));
		}

		@Test
		@DisplayName("GET /api/folders 는 인증 사용자에게 열려 있다")
		void folders_allowAuthenticatedUser() throws Exception {
			assertNotBlocked(get("/api/folders").with(authentication(authenticatedUser())));
		}

		@Test
		@DisplayName("GET /api/folders/tree, PATCH /api/folders/{folderId} 는 인증 사용자에게 열려 있다")
		void folderSubpaths_allowAuthenticatedUser() throws Exception {
			assertNotBlocked(get("/api/folders/tree").with(authentication(authenticatedUser())));
			assertNotBlocked(patch("/api/folders/{folderId}", 1L)
					.with(authentication(authenticatedUser()))
					.contentType(JSON)
					.content("{\"name\":\"backend\"}"));
		}
	}

	@Nested
	@DisplayName("Note 정책")
	class NotePolicy {

		@Test
		@DisplayName("GET /api/notes/{noteId}, GET /api/notes/search 는 익명 접근을 허용한다")
		void noteReadEndpoints_allowAnonymous() throws Exception {
			assertNotBlocked(get("/api/notes/{noteId}", 1L));
			assertNotBlocked(get("/api/notes/search").param("keyword", "테스트"));
		}

		@Test
		@DisplayName("GET /api/notes/me, GET /api/notes/me/search 는 익명 사용자에게 401을 반환한다")
		void myNoteEndpoints_requireAuthentication() throws Exception {
			assertUnauthorized(get("/api/notes/me"));
			assertUnauthorized(get("/api/notes/me/search").param("keyword", "테스트"));
		}

		@Test
		@DisplayName("POST /api/notes, PUT/PATCH/DELETE /api/notes/** 는 익명 사용자에게 401을 반환한다")
		void noteWriteEndpoints_requireAuthentication() throws Exception {
			assertUnauthorized(post("/api/notes")
					.contentType(JSON)
					.content(VALID_NOTE_CREATE_JSON));
			assertUnauthorized(put("/api/notes/{noteId}", 1L)
					.contentType(JSON)
					.content(EMPTY_JSON));
			assertUnauthorized(patch("/api/notes/{noteId}/folder", 1L)
					.contentType(JSON)
					.content(EMPTY_JSON));
			assertUnauthorized(delete("/api/notes/{noteId}", 1L));
		}

		@Test
		@DisplayName("POST /api/notes, GET /api/notes/me 는 인증 사용자에게 열려 있다")
		void protectedNoteEndpoints_allowAuthenticatedUser() throws Exception {
			assertNotBlocked(post("/api/notes")
					.with(authentication(authenticatedUser()))
					.contentType(JSON)
					.content(VALID_NOTE_CREATE_JSON));
			assertNotBlocked(get("/api/notes/me").with(authentication(authenticatedUser())));
		}
	}

	@Nested
	@DisplayName("Fallback 정책 - 명시되지 않은 /api/**")
	class FallbackPolicy {

		@Test
		@DisplayName("GET /api/unknown 은 익명 사용자에게 401을 반환한다")
		void unknownApi_requireAuthentication() throws Exception {
			assertUnauthorized(get("/api/unknown"));
		}

		@Test
		@DisplayName("GET /api/unknown 은 인증 사용자에게 열려 있다")
		void unknownApi_allowAuthenticatedUser() throws Exception {
			assertNotBlocked(get("/api/unknown").with(authentication(authenticatedUser())));
		}
	}

	private void assertNotBlocked(MockHttpServletRequestBuilder requestBuilder) throws Exception {
		mockMvc.perform(requestBuilder)
				.andExpect(notBlockedBySecurity());
	}

	private void assertUnauthorized(MockHttpServletRequestBuilder requestBuilder) throws Exception {
		mockMvc.perform(requestBuilder)
				.andExpect(status().isUnauthorized());
	}

	private ResultMatcher notBlockedBySecurity() {
		return result -> {
			int status = result.getResponse().getStatus();
			assertThat(status)
					.describedAs("security should not block the request")
					.isNotIn(HttpStatus.UNAUTHORIZED.value(), HttpStatus.FORBIDDEN.value());
		};
	}

	private UsernamePasswordAuthenticationToken authenticatedUser() {
		return new UsernamePasswordAuthenticationToken(
				USER_ID,
				null,
				List.of(new SimpleGrantedAuthority("ROLE_USER"))
		);
	}

	@TestConfiguration
	static class TestSupportConfig {
		@Bean
		CorsConfigurationSource corsConfigurationSource() {
			CorsConfiguration configuration = new CorsConfiguration();
			configuration.setAllowedOrigins(List.of("http://localhost:3000"));
			configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
			configuration.setAllowedHeaders(List.of("*"));
			configuration.setAllowCredentials(true);

			UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
			source.registerCorsConfiguration("/**", configuration);
			return source;
		}
	}

	@TestComponent
	@RestController
	static class ProbeController {
		@GetMapping("/__probe")
		void probe() {
		}
	}
}
