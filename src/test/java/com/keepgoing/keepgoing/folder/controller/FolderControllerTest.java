package com.keepgoing.keepgoing.folder.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keepgoing.keepgoing.folder.controller.dto.CreateFolderRequest;
import com.keepgoing.keepgoing.folder.service.FolderService;
import com.keepgoing.keepgoing.folder.service.dto.CreateFolderCommand;
import com.keepgoing.keepgoing.folder.service.dto.FolderResult;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.global.security.jwt.JwtAuthenticationFilter;
import com.keepgoing.keepgoing.user.domain.UserRole;
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

@WebMvcTest(FolderController.class)
@AutoConfigureMockMvc(addFilters = false)
class FolderControllerTest {

	public static final String DIRECTORY_NAME = "backend";
	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@MockitoBean
	FolderService folderService;

	@MockitoBean
	JwtAuthenticationFilter jwtAuthenticationFilter;

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Nested
	@DisplayName("POST /api/folders")
	class PostFolders {

		@Test
		@DisplayName("유효한 요청이면 201 Created를 반환한다.")
		void createFolder_returnsCreated() throws Exception {
			// given
			Long userId = 1L;
			var request = new CreateFolderRequest(null, DIRECTORY_NAME);
			var result = new FolderResult(10L, null, DIRECTORY_NAME);

			mockLoginUser(userId);

			given(folderService.createFolder(any(CreateFolderCommand.class)))
					.willReturn(result);

			// when & then
			mockMvc.perform(post("/api/folders")
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.data.folderId").value(10L))
					.andExpect(jsonPath("$.data.parentFolderId").doesNotExist())
					.andExpect(jsonPath("$.data.name").value(DIRECTORY_NAME));
		}

		@Test
		@DisplayName("이름이 공백이면 400 Bad Request를 반환한다.")
		void createFolder_returnsBadRequestWhenNameIsBlank() throws Exception {
			// given
			Long userId = 1L;
			var request = new CreateFolderRequest(null, "   ");
			mockLoginUser(userId);

			// when & then
			mockMvc.perform(post("/api/folders")
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isBadRequest());
			verifyNoInteractions(folderService);
		}

		@Test
		@DisplayName("이름이 120자를 초과하면 400 Bad Request를 반환한다.")
		void createFolder_returnsBadRequestWhenNameTooLong() throws Exception {
			// given
			Long userId = 1L;
			String tooLongName = "A".repeat(121);
			var request = new CreateFolderRequest(null, tooLongName);
			mockLoginUser(userId);

			// when & then
			mockMvc.perform(post("/api/folders")
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isBadRequest());
			verifyNoInteractions(folderService);
		}

		@Test
		@DisplayName("같은 이름의 폴더가 이미 있으면 409 Conflict를 반환한다.")
		void createFolder_returnsConflictWhenFolderNameDuplicated() throws Exception {
			// given
			Long userId = 1L;
			var request = new CreateFolderRequest(null, DIRECTORY_NAME);
			mockLoginUser(userId);

			given(folderService.createFolder(any(CreateFolderCommand.class)))
					.willThrow(new BusinessException(ErrorCode.FOLDER_NAME_DUPLICATED));

			// when & then
			mockMvc.perform(post("/api/folders")
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value(ErrorCode.FOLDER_NAME_DUPLICATED.toString()));
		}

		@Test
		@DisplayName("폴더 이름에 슬래쉬(/)가 포함되면 400 Bad Request를 반환한다.")
		void createFolder_returnsBadRequestWhenNameContainsSlash() throws Exception {
			// given
			Long userId = 1L;
			var request = new CreateFolderRequest(null, "back/end");
			mockLoginUser(userId);

			given(folderService.createFolder(any(CreateFolderCommand.class)))
					.willThrow(new BusinessException(ErrorCode.FOLDER_INVALID_NAME));

			// when & then
			mockMvc.perform(post("/api/folders")
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value(ErrorCode.FOLDER_INVALID_NAME.toString()));
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