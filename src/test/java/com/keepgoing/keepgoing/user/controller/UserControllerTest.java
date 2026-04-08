package com.keepgoing.keepgoing.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.user.controller.dto.ChangePasswordRequest;
import com.keepgoing.keepgoing.user.controller.dto.UserUpdateRequest;
import com.keepgoing.keepgoing.user.domain.UserRole;
import com.keepgoing.keepgoing.global.security.jwt.JwtAuthenticationFilter;
import com.keepgoing.keepgoing.user.service.dto.ChangePasswordCommand;
import com.keepgoing.keepgoing.user.service.UserService;
import com.keepgoing.keepgoing.user.service.dto.UserInfoResult;
import com.keepgoing.keepgoing.user.service.dto.UserUpdateCommand;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

	private static final String EMAIL = "test@test.com";
	private static final String NAME = "홍길동";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@MockitoBean
	UserService userService;

	@MockitoBean
	JwtAuthenticationFilter jwtAuthenticationFilter;

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Nested
	@DisplayName("GET /api/users/me")
	class GetMyInfo {

		@Test
		@DisplayName("성공 시 200과 사용자 정보를 반환한다.")
		void success() throws Exception {
			Long userId = 1L;
			UserInfoResult result = new UserInfoResult(userId, EMAIL, NAME, UserRole.USER);

			given(userService.getMyInfo(anyLong())).willReturn(result);
			mockLoginUser(userId);

			mockMvc.perform(get("/api/users/me"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.userId").value(userId))
					.andExpect(jsonPath("$.data.email").value(EMAIL))
					.andExpect(jsonPath("$.data.name").value(NAME))
					.andExpect(jsonPath("$.data.role").value(UserRole.USER.toString()));

			then(userService).should().getMyInfo(userId);
		}

		@Test
		@DisplayName("가입된 사용자가 없는 경우 404와 USER_NOT_FOUND를 반환한다.")
		void userNotFound() throws Exception {
			Long userId = 1L;

			willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND))
					.given(userService)
					.getMyInfo(userId);
			mockLoginUser(userId);

			mockMvc.perform(get("/api/users/me"))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value(ErrorCode.USER_NOT_FOUND.name()));

			then(userService).should().getMyInfo(userId);
		}
	}

	@Nested
	@DisplayName("PATCH /api/users/me")
	class PatchMyProfile {

		@Test
		@DisplayName("성공 시 200과 수정된 사용자 정보를 반환한다.")
		void success() throws Exception {
			Long userId = 1L;
			String updatedName = "새 이름";
			UserInfoResult result = new UserInfoResult(userId, EMAIL, updatedName, UserRole.USER);

			given(userService.updateMyProfile(any(UserUpdateCommand.class))).willReturn(result);
			mockLoginUser(userId);

			mockMvc.perform(patch("/api/users/me")
							.contentType(MediaType.APPLICATION_JSON)
							.content(requestJson(new UserUpdateRequest("  새 이름  "))))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.userId").value(userId))
					.andExpect(jsonPath("$.data.email").value(EMAIL))
					.andExpect(jsonPath("$.data.name").value(updatedName))
					.andExpect(jsonPath("$.data.role").value(UserRole.USER.toString()));

			then(userService).should().updateMyProfile(argThat(command ->
					command.userId().equals(userId)
							&& command.name().equals("  새 이름  ")
			));
		}

		@Test
		@DisplayName("이름이 공백이면 400을 반환하고 서비스를 호출하지 않는다.")
		void badRequestWhenNameIsBlank() throws Exception {
			Long userId = 1L;
			mockLoginUser(userId);

			mockMvc.perform(patch("/api/users/me")
							.contentType(MediaType.APPLICATION_JSON)
							.content(requestJson(new UserUpdateRequest("   "))))
					.andExpect(status().isBadRequest());

			then(userService).shouldHaveNoInteractions();
		}

		@Test
		@DisplayName("이름이 100자를 초과하면 400을 반환하고 서비스를 호출하지 않는다.")
		void badRequestWhenNameTooLong() throws Exception {
			Long userId = 1L;
			mockLoginUser(userId);

			mockMvc.perform(patch("/api/users/me")
							.contentType(MediaType.APPLICATION_JSON)
							.content(requestJson(new UserUpdateRequest("가".repeat(101)))))
					.andExpect(status().isBadRequest());

			then(userService).shouldHaveNoInteractions();
		}

		@Test
		@DisplayName("사용자가 없으면 404와 USER_NOT_FOUND를 반환한다.")
		void userNotFound() throws Exception {
			Long userId = 1L;

			willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND))
					.given(userService)
					.updateMyProfile(any(UserUpdateCommand.class));
			mockLoginUser(userId);

			mockMvc.perform(patch("/api/users/me")
							.contentType(MediaType.APPLICATION_JSON)
							.content(requestJson(new UserUpdateRequest("수정할 이름"))))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value(ErrorCode.USER_NOT_FOUND.name()));

			then(userService).should().updateMyProfile(argThat(command ->
					command.userId().equals(userId)
							&& command.name().equals("수정할 이름")
			));
		}
	}

	@Nested
	@DisplayName("POST /api/users/me/change-password")
	class PostChangePassword {

		@Test
		@DisplayName("성공 시 200과 success=true를 반환한다.")
		void success() throws Exception {
			Long userId = 1L;
			mockLoginUser(userId);

			mockMvc.perform(post("/api/users/me/change-password")
							.contentType(MediaType.APPLICATION_JSON)
							.content(requestJson(new ChangePasswordRequest("OldP@ssw0rd!", "NewP@ssw0rd!"))))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data").isEmpty());

			then(userService).should().changePassword(argThat(command ->
					command.userId().equals(userId)
							&& command.currentPassword().equals("OldP@ssw0rd!")
							&& command.newPassword().equals("NewP@ssw0rd!")
			));
		}

		@Test
		@DisplayName("현재 비밀번호가 공백이면 400을 반환하고 서비스를 호출하지 않는다.")
		void badRequestWhenCurrentPasswordIsBlank() throws Exception {
			Long userId = 1L;
			mockLoginUser(userId);

			mockMvc.perform(post("/api/users/me/change-password")
							.contentType(MediaType.APPLICATION_JSON)
							.content(requestJson(new ChangePasswordRequest("   ", "NewP@ssw0rd!"))))
					.andExpect(status().isBadRequest());

			then(userService).shouldHaveNoInteractions();
		}

		@Test
		@DisplayName("새 비밀번호 형식이 잘못되면 400을 반환하고 서비스를 호출하지 않는다.")
		void badRequestWhenNewPasswordInvalid() throws Exception {
			Long userId = 1L;
			mockLoginUser(userId);

			mockMvc.perform(post("/api/users/me/change-password")
							.contentType(MediaType.APPLICATION_JSON)
							.content(requestJson(new ChangePasswordRequest("OldP@ssw0rd!", "password"))))
					.andExpect(status().isBadRequest());

			then(userService).shouldHaveNoInteractions();
		}
	}

	private String requestJson(Object request) throws Exception {
		return objectMapper.writeValueAsString(request);
	}

	private void mockLoginUser(Long userId) {
		Authentication authenticated = UsernamePasswordAuthenticationToken.authenticated(
				userId,
				null,
				List.of(new SimpleGrantedAuthority(UserRole.USER.toAuthority()))
		);
		SecurityContextHolder.getContext().setAuthentication(authenticated);
	}
}
