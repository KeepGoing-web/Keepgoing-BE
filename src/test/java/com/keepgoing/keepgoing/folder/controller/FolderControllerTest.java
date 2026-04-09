package com.keepgoing.keepgoing.folder.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keepgoing.keepgoing.folder.controller.dto.FolderCreateRequest;
import com.keepgoing.keepgoing.folder.controller.dto.FolderRenameRequest;
import com.keepgoing.keepgoing.folder.service.FolderService;
import com.keepgoing.keepgoing.folder.service.dto.FolderCreateCommand;
import com.keepgoing.keepgoing.folder.service.dto.FolderMoveCommand;
import com.keepgoing.keepgoing.folder.service.dto.FolderRenameCommand;
import com.keepgoing.keepgoing.folder.service.dto.FolderSummaryResult;
import com.keepgoing.keepgoing.folder.service.dto.FolderTreeNodeResult;
import com.keepgoing.keepgoing.global.api.exception.GlobalExceptionHandler;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.global.config.JacksonConfig;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FolderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, JacksonConfig.class})
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
			var request = new FolderCreateRequest(null, DIRECTORY_NAME);
			var result = new FolderSummaryResult(10L, null, DIRECTORY_NAME);

			mockLoginUser(userId);

			given(folderService.createFolder(any(FolderCreateCommand.class)))
					.willReturn(result);

			// when & then
			mockMvc.perform(post("/api/folders")
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.data.folderId").value(10L))
					.andExpect(jsonPath("$.data.parentId").value(nullValue()))
					.andExpect(jsonPath("$.data.name").value(DIRECTORY_NAME));
		}

		@Test
		@DisplayName("이름이 공백이면 400 Bad Request를 반환한다.")
		void createFolder_returnsBadRequestWhenNameIsBlank() throws Exception {
			// given
			Long userId = 1L;
			var request = new FolderCreateRequest(null, "   ");
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
			var request = new FolderCreateRequest(null, tooLongName);
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
			var request = new FolderCreateRequest(null, DIRECTORY_NAME);
			mockLoginUser(userId);

			given(folderService.createFolder(any(FolderCreateCommand.class)))
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
			var request = new FolderCreateRequest(null, "back/end");
			mockLoginUser(userId);

			given(folderService.createFolder(any(FolderCreateCommand.class)))
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

	@Nested
	@DisplayName("PATCH /api/folders/{folderId}")
	class PatchFolders {

		@Test
		@DisplayName("유효한 요청이면 200 OK를 반환한다.")
		void renameFolder_returnsOk() throws Exception {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			var request = new FolderRenameRequest("   backend-renamed   ");
			var result = new FolderSummaryResult(folderId, null, "backend-renamed");
			mockLoginUser(userId);

			given(folderService.renameFolder(any(FolderRenameCommand.class)))
					.willReturn(result);

			// when & then
			mockMvc.perform(patch("/api/folders/{folderId}", folderId)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.folderId").value(folderId))
					.andExpect(jsonPath("$.data.parentId").value(nullValue()))
					.andExpect(jsonPath("$.data.name").value("backend-renamed"));

			verify(folderService).renameFolder(argThat(command ->
					command.userId().equals(userId)
							&& command.folderId().equals(folderId)
							&& command.name().equals("   backend-renamed   ")
			));
		}

		@Test
		@DisplayName("이름이 공백이면 400 Bad Request를 반환한다.")
		void renameFolder_returnsBadRequestWhenNameIsBlank() throws Exception {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			var request = new FolderRenameRequest("   ");
			mockLoginUser(userId);

			// when & then
			mockMvc.perform(patch("/api/folders/{folderId}", folderId)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isBadRequest());

			verifyNoInteractions(folderService);
		}

		@Test
		@DisplayName("접근 권한이 없으면 403 Forbidden을 반환한다.")
		void renameFolder_returnsForbiddenWhenAccessDenied() throws Exception {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			var request = new FolderRenameRequest("backend-renamed");
			mockLoginUser(userId);

			given(folderService.renameFolder(any(FolderRenameCommand.class)))
					.willThrow(new BusinessException(ErrorCode.FOLDER_ACCESS_DENIED));

			// when & then
			mockMvc.perform(patch("/api/folders/{folderId}", folderId)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value(ErrorCode.FOLDER_ACCESS_DENIED.toString()));
		}

		@Test
		@DisplayName("폴더가 없으면 404 Not Found를 반환한다.")
		void renameFolder_returnsNotFoundWhenFolderDoesNotExist() throws Exception {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			var request = new FolderRenameRequest("backend-renamed");
			mockLoginUser(userId);

			given(folderService.renameFolder(any(FolderRenameCommand.class)))
					.willThrow(new BusinessException(ErrorCode.FOLDER_NOT_FOUND));

			// when & then
			mockMvc.perform(patch("/api/folders/{folderId}", folderId)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value(ErrorCode.FOLDER_NOT_FOUND.toString()));
		}

		@Test
		@DisplayName("같은 이름의 폴더가 이미 있으면 409 Conflict를 반환한다.")
		void renameFolder_returnsConflictWhenFolderNameDuplicated() throws Exception {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			var request = new FolderRenameRequest("backend-renamed");
			mockLoginUser(userId);

			given(folderService.renameFolder(any(FolderRenameCommand.class)))
					.willThrow(new BusinessException(ErrorCode.FOLDER_NAME_DUPLICATED));

			// when & then
			mockMvc.perform(patch("/api/folders/{folderId}", folderId)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value(ErrorCode.FOLDER_NAME_DUPLICATED.toString()));
		}
	}

	@Nested
	@DisplayName("PATCH /api/folders/{folderId}/parent")
	class PatchFolderParent {

		@Test
		@DisplayName("유효한 요청이면 폴더를 다른 부모 아래로 이동한다.")
		void moveFolder_returnsOk() throws Exception {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			Long parentId = 20L;
			var result = new FolderSummaryResult(folderId, parentId, DIRECTORY_NAME);
			mockLoginUser(userId);

			given(folderService.moveFolder(any(FolderMoveCommand.class)))
					.willReturn(result);

			// when & then
			mockMvc.perform(patch("/api/folders/{folderId}/parent", folderId)
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"parentId": 20}
									"""))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.folderId").value(folderId))
					.andExpect(jsonPath("$.data.parentId").value(parentId))
					.andExpect(jsonPath("$.data.name").value(DIRECTORY_NAME));

			verify(folderService).moveFolder(argThat(command ->
					command.userId().equals(userId)
							&& command.folderId().equals(folderId)
							&& command.parentId().equals(parentId)
			));
		}

		@Test
		@DisplayName("parentId가 null이면 루트로 이동한다.")
		void moveFolder_returnsOkWhenMovingToRoot() throws Exception {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			var result = new FolderSummaryResult(folderId, null, DIRECTORY_NAME);
			mockLoginUser(userId);

			given(folderService.moveFolder(any(FolderMoveCommand.class)))
					.willReturn(result);

			// when & then
			mockMvc.perform(patch("/api/folders/{folderId}/parent", folderId)
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"parentId": null}
									"""))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.folderId").value(folderId))
					.andExpect(jsonPath("$.data.parentId").value(nullValue()))
					.andExpect(jsonPath("$.data.name").value(DIRECTORY_NAME));

			verify(folderService).moveFolder(argThat(command ->
					command.userId().equals(userId)
							&& command.folderId().equals(folderId)
							&& command.parentId() == null
			));
		}

		@Test
		@DisplayName("parentId 필드가 없으면 400 Bad Request를 반환한다.")
		void moveFolder_returnsBadRequestWhenParentIdIsMissing() throws Exception {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			mockLoginUser(userId);

			// when & then
			mockMvc.perform(patch("/api/folders/{folderId}/parent", folderId)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{}"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
					.andExpect(jsonPath("$.error.fieldErrors[0].field").value("parentId"));

			verifyNoInteractions(folderService);
		}

		@Test
		@DisplayName("parentId가 0 이하면 400 Bad Request를 반환한다.")
		void moveFolder_returnsBadRequestWhenParentIdIsNotPositive() throws Exception {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			mockLoginUser(userId);

			// when & then
			mockMvc.perform(patch("/api/folders/{folderId}/parent", folderId)
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"parentId": 0}
									"""))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
					.andExpect(jsonPath("$.error.fieldErrors[0].field").value("parentId"));

			verifyNoInteractions(folderService);
		}

		@Test
		@DisplayName("폴더가 없으면 404 Not Found를 반환한다.")
		void moveFolder_returnsNotFoundWhenFolderDoesNotExist() throws Exception {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			mockLoginUser(userId);

			given(folderService.moveFolder(any(FolderMoveCommand.class)))
					.willThrow(new BusinessException(ErrorCode.FOLDER_NOT_FOUND));

			// when & then
			mockMvc.perform(patch("/api/folders/{folderId}/parent", folderId)
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"parentId": 20}
									"""))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value(ErrorCode.FOLDER_NOT_FOUND.toString()));
		}

		@Test
		@DisplayName("접근 권한이 없으면 403 Forbidden을 반환한다.")
		void moveFolder_returnsForbiddenWhenAccessDenied() throws Exception {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			mockLoginUser(userId);

			given(folderService.moveFolder(any(FolderMoveCommand.class)))
					.willThrow(new BusinessException(ErrorCode.FOLDER_ACCESS_DENIED));

			// when & then
			mockMvc.perform(patch("/api/folders/{folderId}/parent", folderId)
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"parentId": 20}
									"""))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value(ErrorCode.FOLDER_ACCESS_DENIED.toString()));
		}

		@Test
		@DisplayName("허용되지 않는 이동이면 400 Bad Request를 반환한다.")
		void moveFolder_returnsBadRequestWhenMoveIsInvalid() throws Exception {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			mockLoginUser(userId);

			given(folderService.moveFolder(any(FolderMoveCommand.class)))
					.willThrow(new BusinessException(ErrorCode.FOLDER_MOVE_INVALID));

			// when & then
			mockMvc.perform(patch("/api/folders/{folderId}/parent", folderId)
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"parentId": 20}
									"""))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value(ErrorCode.FOLDER_MOVE_INVALID.toString()));
		}

		@Test
		@DisplayName("같은 이름의 폴더가 이미 있으면 409 Conflict를 반환한다.")
		void moveFolder_returnsConflictWhenFolderNameDuplicated() throws Exception {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			mockLoginUser(userId);

			given(folderService.moveFolder(any(FolderMoveCommand.class)))
					.willThrow(new BusinessException(ErrorCode.FOLDER_NAME_DUPLICATED));

			// when & then
			mockMvc.perform(patch("/api/folders/{folderId}/parent", folderId)
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"parentId": 20}
									"""))
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value(ErrorCode.FOLDER_NAME_DUPLICATED.toString()));
		}
	}

	@Nested
	@DisplayName("GET /api/folders")
	class GetFolders {

		@Test
		@DisplayName("parentId가 없으면 루트 폴더 목록을 200 OK로 반환한다.")
		void getFolders_returnsOkForRoot() throws Exception {
			// given
			Long userId = 1L;
			mockLoginUser(userId);

			given(folderService.getFolders(userId, null))
					.willReturn(List.of(
							new FolderSummaryResult(10L, null, "a"),
							new FolderSummaryResult(11L, null, "b")
					));

			// when & then
			mockMvc.perform(get("/api/folders"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data[0].folderId").value(10L))
					.andExpect(jsonPath("$.data[0].parentId").value(nullValue()))
					.andExpect(jsonPath("$.data[0].name").value("a"))
					.andExpect(jsonPath("$.data[1].folderId").value(11L))
					.andExpect(jsonPath("$.data[1].parentId").value(nullValue()))
					.andExpect(jsonPath("$.data[1].name").value("b"));

			verify(folderService).getFolders(userId, null);
		}

		@Test
		@DisplayName("parentId가 있으면 자식 폴더 목록을 200 OK로 반환한다.")
		void getFolders_returnsOkForChildren() throws Exception {
			// given
			Long userId = 1L;
			Long parentId = 100L;
			mockLoginUser(userId);

			given(folderService.getFolders(userId, parentId))
					.willReturn(List.of(
							new FolderSummaryResult(20L, parentId, "backend"),
							new FolderSummaryResult(21L, parentId, "frontend")
					));

			// when & then
			mockMvc.perform(get("/api/folders")
							.param("parentId", String.valueOf(parentId)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data[0].folderId").value(20L))
					.andExpect(jsonPath("$.data[0].parentId").value(parentId))
					.andExpect(jsonPath("$.data[0].name").value("backend"))
					.andExpect(jsonPath("$.data[1].folderId").value(21L))
					.andExpect(jsonPath("$.data[1].parentId").value(parentId))
					.andExpect(jsonPath("$.data[1].name").value("frontend"));

			verify(folderService).getFolders(userId, parentId);
		}
	}

	@Nested
	@DisplayName("GET /api/folders/tree")
	class GetFolderTree {

		@Test
		@DisplayName("전체 트리 조회를 200 OK로 반환한다.")
		void getFolderTree_returnOk() throws Exception {
			//given
			Long userId = 1L;
			mockLoginUser(userId);

			FolderTreeNodeResult spring = new FolderTreeNodeResult(2L, 1L, "spring", List.of());
			FolderTreeNodeResult study = new FolderTreeNodeResult(1L, null, "공부", List.of(spring));

			given(folderService.getFolderTree(userId)).willReturn(List.of(study));

			//when & then
			mockMvc.perform(get("/api/folders/tree"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data[0].folderId").value(1L))
					.andExpect(jsonPath("$.data[0].name").value("공부"))
					.andExpect(jsonPath("$.data[0].children[0].folderId").value(2L))
					.andExpect(jsonPath("$.data[0].children[0].parentId").value(1L))
					.andExpect(jsonPath("$.data[0].children[0].name").value("spring"));

			verify(folderService).getFolderTree(userId);
		}
	}

	@Nested
	@DisplayName("DELETE /api/folders/{folderId}")
	class DeleteFolder {

		@Test
		@DisplayName("유효한 요청이면 204 No Content를 반환한다")
		void deleteFolder_returnsNoContent() throws Exception {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			mockLoginUser(userId);

			// when & then
			mockMvc.perform(delete("/api/folders/{folderId}", folderId))
					.andExpect(status().isNoContent());

			verify(folderService).deleteFolder(userId, folderId);
		}

		@Test
		@DisplayName("폴더가 없으면 404 Not Found를 반환한다")
		void deleteFolder_returnsNotFoundWhenFolderDoesNotExist() throws Exception {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			mockLoginUser(userId);

			willThrow(new BusinessException(ErrorCode.FOLDER_NOT_FOUND))
					.given(folderService).deleteFolder(userId, folderId);

			// when & then
			mockMvc.perform(delete("/api/folders/{folderId}", folderId))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value(ErrorCode.FOLDER_NOT_FOUND.toString()));
		}

		@Test
		@DisplayName("접근 권한이 없으면 403 Forbidden을 반환한다")
		void deleteFolder_returnsForbiddenWhenAccessDenied() throws Exception {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			mockLoginUser(userId);

			willThrow(new BusinessException(ErrorCode.FOLDER_ACCESS_DENIED))
					.given(folderService).deleteFolder(userId, folderId);

			// when & then
			mockMvc.perform(delete("/api/folders/{folderId}", folderId))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value(ErrorCode.FOLDER_ACCESS_DENIED.toString()));
		}

		@Test
		@DisplayName("폴더가 비어있지 않으면 409 Conflict를 반환한다")
		void deleteFolder_returnsConflictWhenFolderIsNotEmpty() throws Exception {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			mockLoginUser(userId);

			willThrow(new BusinessException(ErrorCode.FOLDER_NOT_EMPTY))
					.given(folderService).deleteFolder(userId, folderId);

			// when & then
			mockMvc.perform(delete("/api/folders/{folderId}", folderId))
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value(ErrorCode.FOLDER_NOT_EMPTY.toString()));
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
