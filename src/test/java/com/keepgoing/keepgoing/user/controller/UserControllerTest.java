package com.keepgoing.keepgoing.user.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.user.domain.UserRole;
import com.keepgoing.keepgoing.global.security.jwt.JwtAuthenticationFilter;
import com.keepgoing.keepgoing.user.service.UserService;
import com.keepgoing.keepgoing.user.service.dto.UserInfoResult;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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

	private void mockLoginUser(Long userId) {
		Authentication authenticated = UsernamePasswordAuthenticationToken.authenticated(
				userId,
				null,
				List.of(new SimpleGrantedAuthority(UserRole.USER.toAuthority()))
		);
		SecurityContextHolder.getContext().setAuthentication(authenticated);
	}
}
