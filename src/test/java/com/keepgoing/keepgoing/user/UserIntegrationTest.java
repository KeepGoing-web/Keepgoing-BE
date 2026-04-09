package com.keepgoing.keepgoing.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.user.controller.dto.ChangePasswordRequest;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.domain.UserPasswordCredential;
import com.keepgoing.keepgoing.user.domain.UserRole;
import com.keepgoing.keepgoing.user.repository.UserPasswordCredentialRepository;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class UserIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	UserRepository userRepository;

	@Autowired
	UserPasswordCredentialRepository credentialRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Nested
	@DisplayName("POST /api/users/me/change-password")
	class ChangePassword {

		@Test
		@DisplayName("요청이 유효하면 비밀번호를 변경한다")
		void success() throws Exception {
			User user = savePasswordUser("user@test.com", "홍길동", "OldP@ssw0rd!");
			mockLoginUser(user.getId());

			mockMvc.perform(post("/api/users/me/change-password")
							.contentType(APPLICATION_JSON)
							.content(requestJson(new ChangePasswordRequest("OldP@ssw0rd!", "NewP@ssw0rd!"))))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data").isEmpty());

			UserPasswordCredential credential = credentialRepository.findById(user.getId())
					.orElseThrow();

			assertThat(passwordEncoder.matches("NewP@ssw0rd!", credential.getPasswordHash())).isTrue();
			assertThat(passwordEncoder.matches("OldP@ssw0rd!", credential.getPasswordHash())).isFalse();
		}

		@Test
		@DisplayName("현재 비밀번호가 일치하지 않으면 401을 반환한다")
		void returnsUnauthorizedWhenCurrentPasswordMismatched() throws Exception {
			User user = savePasswordUser("user@test.com", "홍길동", "OldP@ssw0rd!");
			mockLoginUser(user.getId());

			mockMvc.perform(post("/api/users/me/change-password")
							.contentType(APPLICATION_JSON)
							.content(requestJson(new ChangePasswordRequest("WrongP@ssw0rd!", "NewP@ssw0rd!"))))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value(ErrorCode.USER_CURRENT_PASSWORD_MISMATCH.name()));

			UserPasswordCredential credential = credentialRepository.findById(user.getId())
					.orElseThrow();
			assertThat(passwordEncoder.matches("OldP@ssw0rd!", credential.getPasswordHash())).isTrue();
		}

		@Test
		@DisplayName("비밀번호 계정이 없으면 403을 반환한다")
		void returnsForbiddenWhenPasswordCredentialMissing() throws Exception {
			User user = userRepository.save(User.create("oauth@test.com", "홍길동"));
			mockLoginUser(user.getId());

			mockMvc.perform(post("/api/users/me/change-password")
							.contentType(APPLICATION_JSON)
							.content(requestJson(new ChangePasswordRequest("OldP@ssw0rd!", "NewP@ssw0rd!"))))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value(ErrorCode.USER_PASSWORD_CHANGE_NOT_SUPPORTED.name()));
		}
	}

	private User savePasswordUser(String email, String name, String rawPassword) {
		User user = userRepository.save(User.create(email, name));
		UserPasswordCredential credential = UserPasswordCredential.create(
				user,
				email,
				passwordEncoder.encode(rawPassword)
		);
		credentialRepository.saveAndFlush(credential);
		return user;
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
