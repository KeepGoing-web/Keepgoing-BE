package com.keepgoing.keepgoing.auth.controller;

import static com.keepgoing.keepgoing.auth.AuthTestFixtures.signupRequestWithEmail;
import static com.keepgoing.keepgoing.auth.AuthTestFixtures.toJson;
import static com.keepgoing.keepgoing.auth.AuthTestFixtures.validLoginRequest;
import static com.keepgoing.keepgoing.global.common.error.ErrorCode.AUTH_INVALID_CREDENTIALS;
import static com.keepgoing.keepgoing.global.common.error.ErrorCode.AUTH_REFRESH_TOKEN_INVALID;
import static com.keepgoing.keepgoing.global.common.error.ErrorCode.USER_ALREADY_EXISTS;
import static com.keepgoing.keepgoing.global.common.error.ErrorCode.VALIDATION_FAILED;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keepgoing.keepgoing.auth.AuthMapper;
import com.keepgoing.keepgoing.auth.AuthTestFixtures;
import com.keepgoing.keepgoing.auth.controller.dto.LoginRequest;
import com.keepgoing.keepgoing.auth.controller.dto.SignupRequest;
import com.keepgoing.keepgoing.auth.controller.dto.SignupResponse;
import com.keepgoing.keepgoing.auth.service.AuthService;
import com.keepgoing.keepgoing.auth.service.dto.LoginCommand;
import com.keepgoing.keepgoing.auth.service.dto.LoginResult;
import com.keepgoing.keepgoing.auth.service.dto.SignupCommand;
import com.keepgoing.keepgoing.auth.service.dto.SignupResult;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.security.cookie.AuthCookieManager;
import com.keepgoing.keepgoing.global.security.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * WebMvcTest + JPA Auditing 충돌 해결
 * <p>
 * WebMvcTest는 엔티티 스캔을 하지 않아 JPA 메타모델이 비어있는데, EnableJpaAuditing이 메타모델을 요구하여 예외가 발생합니다.
 * <p>
 * 컨트롤러 테스트에서는 실제 JPA 동작이 필요 없으므로, JpaAuditingConfig를 따로 만들어 컨트롤러 테스트 시 해당 설정을 로드하지 않게 합니다.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Security 필터 비활성화
@Import({ObjectMapper.class, AuthMapper.class})
class AuthControllerTest {

	private static final String EMAIL = "test@test.com";
	public static final String ACCESS_TOKEN = "access-token";
	public static final String REFRESH_TOKEN = "refresh-token";
	public static final String NEW_ACCESS_TOKEN = "new-access-token";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@MockitoBean
	AuthService authService;

	@MockitoBean
	JwtAuthenticationFilter jwtAuthenticationFilter;

	@MockitoBean
	AuthCookieManager authCookieManager;

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}


	// 계층 구조로 테스트 코드 가독성 증가
	@Nested
	@DisplayName("POST /api/auth/signup - 회원가입")
	class Signup {

		@Test
		@DisplayName("성공 시 201과 SignupResponse를 반환한다.")
		void success() throws Exception {
			// given
			SignupRequest request = AuthTestFixtures.validSignupRequest();
			SignupResult result = new SignupResult(
					1L,
					request.email(),
					request.name()
			);
			SignupResponse response = new SignupResponse(
					result.id(),
					request.email(),
					request.name()
			);

			given(authService.signup(any(SignupCommand.class)))
					.willReturn(result);

			String json = toJson(objectMapper, request);

			// when & then
			mockMvc.perform(post("/api/auth/signup")
							.contentType(MediaType.APPLICATION_JSON)
							.content(json))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.id").value(1L))
					.andExpect(jsonPath("$.data.email").value(request.email()))
					.andExpect(jsonPath("$.data.name").value(request.name()));
		}

		@Test
		@DisplayName("이메일이 이미 존재하면 409와 USER_ALREADY_EXISTS 에러를 반환한다.")
		void email_duplicate() throws Exception {
			// given
			SignupRequest request = signupRequestWithEmail("dup@example.com");
			String json = toJson(objectMapper, request);

			willThrow(new BusinessException(USER_ALREADY_EXISTS))
					.given(authService)
					.signup(any(SignupCommand.class));

			// when & then
			mockMvc.perform(post("/api/auth/signup")
							.contentType(MediaType.APPLICATION_JSON)
							.content(json))
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value(USER_ALREADY_EXISTS.name()));
		}

		@Test
		@DisplayName("검증 실패 시 400과 VALIDATION_FAILED, FieldErrors 배열을 반환한다.")
		void validation_failed() throws Exception {
			// given
			var invalid = SignupRequest.builder()
					.email("not-an-email")
					.password("")
					.name("")
					.build();
			var json = toJson(objectMapper, invalid);

			// when & then
			mockMvc.perform(post("/api/auth/signup")
							.contentType(MediaType.APPLICATION_JSON)
							.content(json))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value(VALIDATION_FAILED.name()))
					.andExpect(jsonPath("$.error.fieldErrors").isArray());
		}
	}

	@Nested
	@DisplayName("POST /api/auth/login - 로그인")
	class Login {

		@Test
		@DisplayName("성공 시 200 응답과 함께 access/refresh 토큰 쿠키를 발급한다.")
		void success() throws Exception {
			// given
			LoginRequest request = validLoginRequest();
			LoginResult result = new LoginResult(
					ACCESS_TOKEN,
					REFRESH_TOKEN,
					1L,
					EMAIL
			);

			given(authService.login(any(LoginCommand.class)))
					.willReturn(result);

			String json = toJson(objectMapper, request);

			// when & then
			mockMvc.perform(post("/api/auth/login")
							.contentType(MediaType.APPLICATION_JSON)
							.content(json)
					).andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.userId").value(1L))
					.andExpect(jsonPath("$.data.email").value(EMAIL));

			then(authCookieManager).should().addAccessToken(any(HttpServletResponse.class), eq(ACCESS_TOKEN));
			then(authCookieManager).should().addRefreshToken(any(HttpServletResponse.class), eq(REFRESH_TOKEN));
		}

		@Test
		@DisplayName("잘못된 자격 증명 시 401과 AUTH_INVALID_CREDENTIALS 에러를 반환한다.")
		void invalid_credentials() throws Exception {
			// given
			LoginRequest request = validLoginRequest();
			String json = toJson(objectMapper, request);

			willThrow(new BadCredentialsException("Bad credentials"))
					.given(authService)
					.login(any(LoginCommand.class));

			// when & then
			mockMvc.perform(post("/api/auth/login")
							.contentType(MediaType.APPLICATION_JSON)
							.content(json))
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value(AUTH_INVALID_CREDENTIALS.name()));

			then(authCookieManager).should(never()).addAccessToken(any(), anyString());
			then(authCookieManager).should(never()).addRefreshToken(any(), anyString());
		}

		@Test
		@DisplayName("검증 실패 시 400과 VALIDATION_FAILED를 반환한다.")
		void validation_failed() throws Exception {
			// given
			LoginRequest invalidRequest = new LoginRequest("", "");
			String json = toJson(objectMapper, invalidRequest);

			// when & then
			mockMvc.perform(post("/api/auth/login")
							.contentType(MediaType.APPLICATION_JSON)
							.content(json))
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value(VALIDATION_FAILED.name()));
		}
	}


	@Nested
	@DisplayName("POST /api/auth/refresh")
	class Refresh {

		@Test
		@DisplayName("성공 시 200 + access token 재발급")
		void refresh_success() throws Exception {
			// given
			given(authCookieManager.extractRefreshToken(any()))
					.willReturn(Optional.of(REFRESH_TOKEN));
			given(authService.refresh(REFRESH_TOKEN))
					.willReturn(NEW_ACCESS_TOKEN);

			// when & then
			mockMvc.perform(post("/api/auth/refresh"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true));

			then(authService).should().refresh(REFRESH_TOKEN);
			then(authCookieManager).should()
					.addAccessToken(any(HttpServletResponse.class), eq(NEW_ACCESS_TOKEN));
		}

		@Test
		@DisplayName("refresh 쿠기가 없으면 401을 반환한다.")
		void refresh_token_missing() throws Exception {
			given(authCookieManager.extractRefreshToken(any()))
					.willReturn(Optional.empty());

			// when & then
			mockMvc.perform(post("/api/auth/refresh"))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value(AUTH_REFRESH_TOKEN_INVALID.name()));

			then(authService).shouldHaveNoInteractions();
			then(authCookieManager).should(never())
					.addAccessToken(any(HttpServletResponse.class), anyString());
		}

		@Test
		@DisplayName("유효하지 않은 refresh token이면 401을 반환한다.")
		void invalid_refresh_token() throws Exception {
			// given
			given(authCookieManager.extractRefreshToken(any()))
					.willReturn(Optional.of("bad-refresh-token"));
			willThrow(new BusinessException(AUTH_REFRESH_TOKEN_INVALID))
					.given(authService).refresh("bad-refresh-token");

			// when & then
			mockMvc.perform(post("/api/auth/refresh"))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value(AUTH_REFRESH_TOKEN_INVALID.name()));

			then(authCookieManager).should(never()).addAccessToken(any(), anyString());
		}
	}

	@Nested
	@DisplayName("POST /api/auth/logout")
	class Logout {

		@Test
		@DisplayName("성공 시 200을 반환하고 토큰 쿠키를 만료시킨다.")
		void logout_success() throws Exception {
			// when & then
			mockMvc.perform(post("/api/auth/logout"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true));

			then(authService).shouldHaveNoInteractions();
			then(authCookieManager).should().clearAllTokens(any(HttpServletResponse.class));
		}
	}
}
