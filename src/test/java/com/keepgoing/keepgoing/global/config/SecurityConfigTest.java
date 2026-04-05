package com.keepgoing.keepgoing.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keepgoing.keepgoing.auth.AuthMapper;
import com.keepgoing.keepgoing.auth.controller.dto.MyInfoResponse;
import com.keepgoing.keepgoing.auth.service.AuthService;
import com.keepgoing.keepgoing.auth.service.dto.MyInfoResult;
import com.keepgoing.keepgoing.folder.service.FolderService;
import com.keepgoing.keepgoing.folder.service.dto.FolderSummaryResult;
import com.keepgoing.keepgoing.global.security.cookie.AuthCookieManager;
import com.keepgoing.keepgoing.note.domain.NoteVisibility;
import com.keepgoing.keepgoing.note.service.NoteService;
import com.keepgoing.keepgoing.note.service.dto.NoteCreateCommand;
import com.keepgoing.keepgoing.note.service.dto.NoteDetailResult;
import com.keepgoing.keepgoing.note.service.dto.NoteSearchQuery;
import com.keepgoing.keepgoing.note.service.dto.NoteSummaryResult;
import com.keepgoing.keepgoing.user.domain.UserRole;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
		"spring.security.oauth2.client.registration.google.client-id=test-client",
		"spring.security.oauth2.client.registration.google.client-secret=test-secret"
})
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
	NoteService noteService;

	@MockitoBean
	FolderService folderService;

	@MockitoBean
	AuthService authService;

	@MockitoBean
	AuthMapper authMapper;

	@MockitoBean
	AuthCookieManager authCookieManager;

	@Nested
	@DisplayName("permitAll 경로")
	class PermitAllEndpoints {

		@Test
		@DisplayName("Swagger 문서는 익명 접근을 허용한다")
		void swaggerDocs_allowAnonymous() throws Exception {
			assertPermitted(get("/v3/api-docs"));
			assertPermitted(get("/swagger-ui/index.html"));
		}

		@Test
		@DisplayName("회원가입과 로그인 API는 익명 접근을 허용한다")
		void authEndpoints_allowAnonymous() throws Exception {
			mockMvc.perform(post("/api/auth/signup")
							.contentType(JSON)
							.content(EMPTY_JSON))
					.andExpect(status().isBadRequest());

			mockMvc.perform(post("/api/auth/login")
							.contentType(JSON)
							.content(EMPTY_JSON))
					.andExpect(status().isBadRequest());

			verifyNoInteractions(authService, authMapper);
		}

		@Test
		@DisplayName("토큰 갱신과 로그아웃 API는 익명 접근을 허용한다")
		void refreshAndLogout_allowAnonymous() throws Exception {
			given(authCookieManager.extractRefreshToken(any())).willReturn(Optional.of("refresh-token"));
			given(authService.refresh("refresh-token")).willReturn("access-token");

			mockMvc.perform(post("/api/auth/refresh"))
					.andExpect(status().isOk());

			mockMvc.perform(post("/api/auth/logout"))
					.andExpect(status().isOk());
		}

		@Test
		@DisplayName("OAuth2 진입 경로는 익명 접근을 허용한다")
		void oauthEndpoints_allowAnonymous() throws Exception {
			assertPermitted(get("/oauth2/authorization/google"));
			assertPermitted(get("/login/oauth2/code/google")
					.param("code", "test-code")
					.param("state", "test-state"));
		}

		@Test
		@DisplayName("노트 공개 조회 경로는 익명 접근을 허용한다")
		void noteReadEndpoints_allowAnonymous() throws Exception {
			given(noteService.getNote(1L)).willReturn(noteDetail(1L));
			given(noteService.searchNote(any(NoteSearchQuery.class)))
					.willReturn(new PageImpl<>(List.of(noteSummary(1L))));

			mockMvc.perform(get("/api/notes/{noteId}", 1L))
					.andExpect(status().isOk());

			mockMvc.perform(get("/api/notes/search").param("keyword", "테스트"))
					.andExpect(status().isOk());
		}
	}

	@Nested
	@DisplayName("익명 사용자는 보호된 경로에 접근할 수 없다")
	class ProtectedEndpointsForAnonymous {

		@Test
		@DisplayName("노트 쓰기 및 내 노트 경로는 인증이 필요하다")
		void noteProtectedEndpoints_requireAuthentication() throws Exception {
			mockMvc.perform(post("/api/notes")
							.contentType(JSON)
							.content(VALID_NOTE_CREATE_JSON))
					.andExpect(status().isUnauthorized());

			mockMvc.perform(put("/api/notes/{noteId}", 1L)
							.contentType(JSON)
							.content(EMPTY_JSON))
					.andExpect(status().isUnauthorized());

			mockMvc.perform(patch("/api/notes/{noteId}/folder", 1L)
							.contentType(JSON)
							.content(EMPTY_JSON))
					.andExpect(status().isUnauthorized());

			mockMvc.perform(delete("/api/notes/{noteId}", 1L))
					.andExpect(status().isUnauthorized());

			mockMvc.perform(get("/api/notes/me"))
					.andExpect(status().isUnauthorized());

			mockMvc.perform(get("/api/notes/me/search").param("keyword", "테스트"))
					.andExpect(status().isUnauthorized());

			verifyNoInteractions(noteService);
		}

		@Test
		@DisplayName("폴더 경로는 인증이 필요하다")
		void folderEndpoints_requireAuthentication() throws Exception {
			mockMvc.perform(get("/api/folders"))
					.andExpect(status().isUnauthorized());

			mockMvc.perform(post("/api/folders")
							.contentType(JSON)
							.content("{\"name\":\"backend\"}"))
					.andExpect(status().isUnauthorized());

			verifyNoInteractions(folderService);
		}

		@Test
		@DisplayName("명시되지 않은 기본 API 경로는 인증이 필요하다")
		void fallbackApiEndpoints_requireAuthentication() throws Exception {
			mockMvc.perform(get("/api/auth/me"))
					.andExpect(status().isUnauthorized());

			mockMvc.perform(get("/api/unknown"))
					.andExpect(status().isUnauthorized());

			verifyNoInteractions(authService, authMapper);
		}
	}

	@Nested
	@DisplayName("인증 사용자는 보호된 경로를 통과한다")
	class ProtectedEndpointsForAuthenticatedUser {

		@Test
		@DisplayName("인증 사용자는 노트를 생성할 수 있다")
		void createNote_allowsAuthenticatedUser() throws Exception {
			given(noteService.createNote(any(NoteCreateCommand.class))).willReturn(noteDetail(1L));

			mockMvc.perform(post("/api/notes")
							.with(authentication(authenticatedUser()))
							.contentType(JSON)
							.content(VALID_NOTE_CREATE_JSON))
					.andExpect(status().isCreated());
		}

		@Test
		@DisplayName("인증 사용자는 폴더 목록을 조회할 수 있다")
		void getFolders_allowsAuthenticatedUser() throws Exception {
			given(folderService.getFolders(USER_ID, null))
					.willReturn(List.of(new FolderSummaryResult(1L, null, "backend")));

			mockMvc.perform(get("/api/folders")
							.with(authentication(authenticatedUser())))
					.andExpect(status().isOk());
		}

		@Test
		@DisplayName("인증 사용자는 내 정보 경로에 접근할 수 있다")
		void fallbackAuthenticatedEndpoint_allowsAuthenticatedUser() throws Exception {
			MyInfoResult myInfoResult = new MyInfoResult(USER_ID, "user@test.com", "tester", UserRole.USER);
			MyInfoResponse myInfoResponse = new MyInfoResponse(USER_ID, "user@test.com", "tester", UserRole.USER);
			given(authService.getMyInfo(USER_ID)).willReturn(myInfoResult);
			given(authMapper.toResponse(eq(myInfoResult))).willReturn(myInfoResponse);

			mockMvc.perform(get("/api/auth/me")
							.with(authentication(authenticatedUser())))
					.andExpect(status().isOk());
		}
	}

	private void assertPermitted(MockHttpServletRequestBuilder requestBuilder) throws Exception {
		mockMvc.perform(requestBuilder)
				.andExpect(notBlockedBySecurity());
	}

	private ResultMatcher notBlockedBySecurity() {
		return result -> {
			int status = result.getResponse().getStatus();

			assertThat(status)
					.describedAs("security should not block the request")
					.isNotIn(HttpStatus.UNAUTHORIZED.value(), HttpStatus.FORBIDDEN.value());
			assertThat(status)
					.describedAs("permitted request should not fail with a server error")
					.isLessThan(HttpStatus.INTERNAL_SERVER_ERROR.value());
		};
	}

	private UsernamePasswordAuthenticationToken authenticatedUser() {
		return new UsernamePasswordAuthenticationToken(
				USER_ID,
				null,
				List.of(new SimpleGrantedAuthority("ROLE_USER"))
		);
	}

	private NoteDetailResult noteDetail(Long noteId) {
		return new NoteDetailResult(
				noteId,
				null,
				USER_ID,
				"제목",
				"내용",
				NoteVisibility.PUBLIC,
				true,
				null,
				null
		);
	}

	private NoteSummaryResult noteSummary(Long noteId) {
		return new NoteSummaryResult(
				noteId,
				null,
				"제목",
				NoteVisibility.PUBLIC,
				true,
				null
		);
	}
}
