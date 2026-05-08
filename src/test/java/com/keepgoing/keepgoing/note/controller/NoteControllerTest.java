package com.keepgoing.keepgoing.note.controller;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keepgoing.keepgoing.global.api.exception.GlobalExceptionHandler;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.global.config.JacksonConfig;
import com.keepgoing.keepgoing.global.security.cookie.CookieConfig;
import com.keepgoing.keepgoing.global.security.jwt.JwtProvider;
import com.keepgoing.keepgoing.note.controller.dto.request.NoteCreateRequest;
import com.keepgoing.keepgoing.note.controller.dto.request.NoteRenameRequest;
import com.keepgoing.keepgoing.note.controller.dto.request.NoteUpdateRequest;
import com.keepgoing.keepgoing.note.domain.NoteVisibility;
import com.keepgoing.keepgoing.note.service.NoteService;
import com.keepgoing.keepgoing.note.service.dto.NoteCreateCommand;
import com.keepgoing.keepgoing.note.service.dto.NoteDetailResult;
import com.keepgoing.keepgoing.note.service.dto.NoteMoveCommand;
import com.keepgoing.keepgoing.note.service.dto.NoteRenameCommand;
import com.keepgoing.keepgoing.note.service.dto.NoteSearchQuery;
import com.keepgoing.keepgoing.note.service.dto.NoteSummaryResult;
import com.keepgoing.keepgoing.note.service.dto.NoteUpdateCommand;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(NoteController.class)
@Import({GlobalExceptionHandler.class, CookieConfig.class, JacksonConfig.class})
public class NoteControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@MockitoBean
	NoteService noteService;

	@MockitoBean
	JpaMetamodelMappingContext jpaMappingContext;

	@MockitoBean
	JwtProvider jwtProvider;

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}
	// ========== Note ==========

	@Nested
	@DisplayName("POST /api/notes")
	class Create {

		@Test
		@DisplayName("folderId가 null이면 루트 노트 생성 요청이 서비스로 전달된다")
		void createNote_successWhenFolderIdIsNull() throws Exception {
			//given
			Long userId = 1L;
			NoteCreateRequest request = new NoteCreateRequest(
					null,
					"테스트 제목",
					"테스트 내용",
					NoteVisibility.PRIVATE,
					true
			);

			NoteDetailResult result = new NoteDetailResult(
					1L,
					null,
					userId,
					"테스트 제목",
					"테스트 내용",
					NoteVisibility.PRIVATE,
					true,
					null,
					null
			);

			given(noteService.createNote(any(NoteCreateCommand.class)))
					.willReturn(result);

			SecurityContextHolder.getContext()
					.setAuthentication(
							new UsernamePasswordAuthenticationToken(userId, null, List.of())
					);

			String json = objectMapper.writeValueAsString(request);

			//when & then
			mockMvc.perform(post("/api/notes")
							.contentType(MediaType.APPLICATION_JSON)
							.content(json))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.noteId").value(1L))
					.andExpect(jsonPath("$.data.title").value("테스트 제목"))
					.andExpect(jsonPath("$.data.folderId").isEmpty())
					.andExpect(jsonPath("$.data.userId").value(1L));

			ArgumentCaptor<NoteCreateCommand> captor = ArgumentCaptor.forClass(NoteCreateCommand.class);
			verify(noteService).createNote(captor.capture());
			assertThat(captor.getValue()
					.userId()).isEqualTo(userId);
			assertThat(captor.getValue()
					.folderId()).isNull();
			assertThat(captor.getValue()
					.title()).isEqualTo("테스트 제목");
		}

		@Test
		@DisplayName("folderId가 있으면 해당 값이 서비스와 응답에 반영된다")
		void createNote_successWhenFolderIdIsProvided() throws Exception {
			//given
			Long userId = 1L;
			Long folderId = 10L;
			NoteCreateRequest request = new NoteCreateRequest(
					folderId,
					"테스트 제목",
					"테스트 내용",
					NoteVisibility.PRIVATE,
					true
			);

			NoteDetailResult result = new NoteDetailResult(
					1L,
					folderId,
					userId,
					"테스트 제목",
					"테스트 내용",
					NoteVisibility.PRIVATE,
					true,
					null,
					null
			);

			given(noteService.createNote(any(NoteCreateCommand.class)))
					.willReturn(result);

			SecurityContextHolder.getContext()
					.setAuthentication(
							new UsernamePasswordAuthenticationToken(userId, null, List.of())
					);

			String json = objectMapper.writeValueAsString(request);

			//when & then
			mockMvc.perform(post("/api/notes")
							.contentType(MediaType.APPLICATION_JSON)
							.content(json))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.noteId").value(1L))
					.andExpect(jsonPath("$.data.folderId").value(folderId))
					.andExpect(jsonPath("$.data.userId").value(userId))
					.andExpect(jsonPath("$.data.title").value("테스트 제목"));

			ArgumentCaptor<NoteCreateCommand> captor = ArgumentCaptor.forClass(NoteCreateCommand.class);
			verify(noteService).createNote(captor.capture());
			assertThat(captor.getValue()
					.userId()).isEqualTo(userId);
			assertThat(captor.getValue()
					.folderId()).isEqualTo(folderId);
		}

		@Test
		@DisplayName("존재하지 않는 폴더면 404 에러 응답")
		void createNote_returnsNotFoundWhenFolderDoesNotExist() throws Exception {
			// given
			Long userId = 1L;
			NoteCreateRequest request = new NoteCreateRequest(
					10L,
					"테스트 제목",
					"테스트 내용",
					NoteVisibility.PRIVATE,
					true
			);

			given(noteService.createNote(any(NoteCreateCommand.class)))
					.willThrow(new BusinessException(ErrorCode.FOLDER_NOT_FOUND));

			SecurityContextHolder.getContext()
					.setAuthentication(
							new UsernamePasswordAuthenticationToken(userId, null, List.of())
					);

			String json = objectMapper.writeValueAsString(request);

			// when & then
			mockMvc.perform(post("/api/notes")
							.contentType(MediaType.APPLICATION_JSON)
							.content(json))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("FOLDER_NOT_FOUND"));
		}

		@Test
		@DisplayName("다른 사용자의 폴더면 403 에러 응답")
		void createNote_returnsForbiddenWhenFolderOwnedByAnotherUser() throws Exception {
			// given
			Long userId = 1L;
			NoteCreateRequest request = new NoteCreateRequest(
					10L,
					"테스트 제목",
					"테스트 내용",
					NoteVisibility.PRIVATE,
					true
			);

			given(noteService.createNote(any(NoteCreateCommand.class)))
					.willThrow(new BusinessException(ErrorCode.FOLDER_ACCESS_DENIED));

			SecurityContextHolder.getContext()
					.setAuthentication(
							new UsernamePasswordAuthenticationToken(userId, null, List.of())
					);

			String json = objectMapper.writeValueAsString(request);

			// when & then
			mockMvc.perform(post("/api/notes")
							.contentType(MediaType.APPLICATION_JSON)
							.content(json))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("FOLDER_ACCESS_DENIED"));
		}

	}

	// ========== GET ==========

	@Nested
	@DisplayName("GET /api/notes/{noteId}")
	class Get {

		@Test
		@DisplayName("익명 사용자는 공개 노트를 조회할 수 있다")
		void anonymousUser_canReadPublicNote() throws Exception {
			Long noteId = 1L;

			NoteDetailResult result = new NoteDetailResult(
					noteId,
					1L,
					2L,
					"테스트 제목",
					"테스트 내용",
					NoteVisibility.PUBLIC,
					true,
					null,
					null
			);

			given(noteService.getNote(isNull(), eq(noteId))).willReturn(result);

			mockMvc.perform(get("/api/notes/{noteId}", noteId))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.title").value("테스트 제목"))
					.andExpect(jsonPath("$.data.visibility").value("PUBLIC"));
		}

		@Test
		@DisplayName("익명 사용자가 비공개 노트를 조회하면 403을 반환한다")
		void anonymousUser_cannotReadPrivateNote() throws Exception {
			Long noteId = 1L;

			given(noteService.getNote(isNull(), eq(noteId)))
					.willThrow(new BusinessException(ErrorCode.NOTE_ACCESS_DENIED));

			mockMvc.perform(get("/api/notes/{noteId}", noteId))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("NOTE_ACCESS_DENIED"));
		}

		@Test
		@DisplayName("인증 사용자는 본인 비공개 노트를 조회할 수 있다")
		void authenticatedUser_canReadOwnPrivateNote() throws Exception {
			Long userId = 1L;
			Long noteId = 1L;

			SecurityContextHolder.getContext()
					.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));

			NoteDetailResult result = new NoteDetailResult(
					noteId,
					1L,
					userId,
					"내 비공개 노트",
					"테스트 내용",
					NoteVisibility.PRIVATE,
					true,
					null,
					null
			);

			given(noteService.getNote(eq(userId), eq(noteId))).willReturn(result);

			mockMvc.perform(get("/api/notes/{noteId}", noteId))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.visibility").value("PRIVATE"));
		}

		@Test
		@DisplayName("인증 사용자는 타인의 공개 노트를 조회할 수 있다")
		void authenticatedUser_canReadOthersPublicNote() throws Exception {
			Long userId = 1L;
			Long noteId = 2L;

			SecurityContextHolder.getContext()
					.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));

			NoteDetailResult result = new NoteDetailResult(
					noteId,
					null,
					2L,
					"타인 공개 노트",
					"테스트 내용",
					NoteVisibility.PUBLIC,
					true,
					null,
					null
			);

			given(noteService.getNote(eq(userId), eq(noteId))).willReturn(result);

			mockMvc.perform(get("/api/notes/{noteId}", noteId))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.visibility").value("PUBLIC"));
		}

		@Test
		@DisplayName("인증 사용자가 타인의 비공개 노트를 조회하면 403을 반환한다")
		void authenticatedUser_cannotReadOthersPrivateNote() throws Exception {
			Long userId = 1L;
			Long noteId = 2L;

			SecurityContextHolder.getContext()
					.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));

			given(noteService.getNote(eq(userId), eq(noteId)))
					.willThrow(new BusinessException(ErrorCode.NOTE_ACCESS_DENIED));

			mockMvc.perform(get("/api/notes/{noteId}", noteId))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("NOTE_ACCESS_DENIED"));
		}

		@Test
		@DisplayName("없는 노트를 조회하면 찾을 수 없음을 응답한다")
		void returnsNotFoundWhenNoteDoesNotExist() throws Exception {
			Long noteId = 999L;

			given(noteService.getNote(isNull(), eq(noteId)))
					.willThrow(new BusinessException(ErrorCode.NOTE_NOT_FOUND));

			mockMvc.perform(get("/api/notes/{noteId}", noteId))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("NOTE_NOT_FOUND"));
		}

	}

	// ========== GET /api/notes/me ==========

	@Nested
	@DisplayName("GET /api/notes/me")
	class GetAll {

		@Test
		@DisplayName("내 게시글 페이지네이션 조회 성공")
		void success() throws Exception {
			// given
			Long userId = 1L;

			var notes = List.of(
					new NoteSummaryResult(1L, 10L, "제목1", NoteVisibility.PRIVATE, true, null),
					new NoteSummaryResult(2L, null, "제목2", NoteVisibility.PUBLIC, true, null)
			);

			Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
			Page<NoteSummaryResult> notePage = new PageImpl<>(notes, pageable, notes.size());

			given(noteService.getNotes(eq(userId), any(Pageable.class)))
					.willReturn(notePage);

			SecurityContextHolder.getContext()
					.setAuthentication(
							new UsernamePasswordAuthenticationToken(userId, null, List.of())
					);

			// when & then
			mockMvc.perform(get("/api/notes/me")
							.param("page", "0")
							.param("size", "10"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.contents").isArray())
					.andExpect(jsonPath("$.data.contents.length()").value(2))
					.andExpect(jsonPath("$.data.page").value(0))
					.andExpect(jsonPath("$.data.size").value(10))
					.andExpect(jsonPath("$.data.contents[0].title").value("제목1"))
					.andExpect(jsonPath("$.data.contents[0].folderId").value(10L))
					.andExpect(jsonPath("$.data.contents[1].folderId").isEmpty())
					.andExpect(jsonPath("$.data.totalElements").value(2))
					.andExpect(jsonPath("$.data.totalPages").value(1))
					.andExpect(jsonPath("$.data.last").value(true));
			verify(noteService).getNotes(eq(userId), any(Pageable.class));
		}

		@Test
		@DisplayName("size가 MAX_PAGE_SIZE보다 크면 상한으로 제한된다")
		void clampsPageSize() throws Exception {
			// given
			Long userId = 1L;

			given(noteService.getNotes(eq(userId), any(Pageable.class)))
					.willReturn(Page.empty());

			SecurityContextHolder.getContext()
					.setAuthentication(
							new UsernamePasswordAuthenticationToken(userId, null, List.of())
					);

			// when
			mockMvc.perform(get("/api/notes/me")
							.param("page", "0")
							.param("size", "1000"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true));

			// then
			ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
			verify(noteService).getNotes(eq(userId), captor.capture());

			assertThat(captor.getValue()
					.getPageSize()).isEqualTo(100);
		}

	}

	// ========== PUT /api/notes/{noteId} ==========

	@Nested
	@DisplayName("PUT /api/notes/{noteId}")
	class Update {

		@Test
		@DisplayName("노트 수정 성공")
		void success() throws Exception {
			// given
			Long noteId = 1L;
			Long userId = 1L;

			NoteUpdateRequest request = new NoteUpdateRequest(
					"수정된 제목",
					"수정된 내용",
					NoteVisibility.PUBLIC,
					false
			);

			NoteDetailResult result = new NoteDetailResult(
					userId,
					noteId,
					null,
					"수정된 제목",
					"수정된 내용",
					NoteVisibility.PUBLIC,
					false,
					null,
					null
			);

			given(noteService.updateNote(any(NoteUpdateCommand.class)))
					.willReturn(result);

			String json = objectMapper.writeValueAsString(request);

			// when & then
			mockMvc.perform(put("/api/notes/{noteId}", noteId)
							.contentType(MediaType.APPLICATION_JSON)
							.content(json))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.title").value("수정된 제목"));
		}

		@Test
		@DisplayName("수정 시 제목과 본문을 비워두어도 성공한다")
		void updateNote_successWithEmptyFields() throws Exception {
			// given
			Long noteId = 1L;
			Long userId = 1L;
			NoteUpdateRequest request = new NoteUpdateRequest("", "", NoteVisibility.PRIVATE, false);
			NoteDetailResult result = new NoteDetailResult(userId, noteId, null, "", "", NoteVisibility.PRIVATE, false, null, null);

			given(noteService.updateNote(any(NoteUpdateCommand.class))).willReturn(result);
			SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));

			// when & then
			mockMvc.perform(put("/api/notes/{noteId}", noteId)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.title").value(""))
					.andExpect(jsonPath("$.data.content").value(""));
		}

		@Test
		@DisplayName("빈 제목으로 PUBLIC 업데이트 시도 시 에러를 반환한다")
		void updateNote_failWhenPublicWithEmptyTitle() throws Exception {
			// given
			Long noteId = 1L;
			NoteUpdateRequest request = new NoteUpdateRequest("", "내용", NoteVisibility.PUBLIC, false);

			given(noteService.updateNote(any())).willThrow(new BusinessException(ErrorCode.NOTE_TITLE_INVALID));

			// when & then
			mockMvc.perform(put("/api/notes/{noteId}", noteId)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.error.code").value("NOTE_TITLE_INVALID"));
		}

		@Test
		@DisplayName("다른 사용자가 노트 수정을 요청하면 권한 오류를 응답한다")
		void accessDenied() throws Exception {
			// given
			Long noteId = 1L;

			NoteUpdateRequest request = new NoteUpdateRequest(
					"남의 글 수정",
					"이건 실패해야 함",
					NoteVisibility.PUBLIC,
					true
			);

			given(noteService.updateNote(any(NoteUpdateCommand.class)))
					.willThrow(new BusinessException(ErrorCode.NOTE_ACCESS_DENIED));

			String json = objectMapper.writeValueAsString(request);

			// when & then
			mockMvc.perform(put("/api/notes/{noteId}", noteId)
							.contentType(MediaType.APPLICATION_JSON)
							.content(json))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("NOTE_ACCESS_DENIED"));
		}

	}

	// ========== DELETE /api/notes/{noteId} ==========

	@Nested
	@DisplayName("DELETE /api/notes/{noteId}")
	class Delete {

		@Test
		@DisplayName("노트 삭제 성공")
		void success() throws Exception {
			// given
			Long noteId = 1L;
			Long userId = 1L;

			SecurityContextHolder.getContext()
					.setAuthentication(
							new UsernamePasswordAuthenticationToken(userId, null, List.of())
					);

			willDoNothing().given(noteService)
					.deleteNote(userId, noteId);

			// when & then
			mockMvc.perform(delete("/api/notes/{noteId}", noteId))
					.andExpect(status().isNoContent())
					.andExpect(content().string(""));

			verify(noteService).deleteNote(userId, noteId);
		}

		@Test
		@DisplayName("다른 사용자가 노트 삭제를 요청하면 권한 오류를 응답한다")
		void accessDenied() throws Exception {
			// given
			Long noteId = 1L;
			Long userId = 1L;

			SecurityContextHolder.getContext()
					.setAuthentication(
							new UsernamePasswordAuthenticationToken(userId, null, List.of())
					);

			willThrow(new BusinessException(ErrorCode.NOTE_ACCESS_DENIED))
					.given(noteService)
					.deleteNote(userId, noteId);

			// when & then
			mockMvc.perform(delete("/api/notes/{noteId}", noteId))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("NOTE_ACCESS_DENIED"));
		}

	}

	// ========== PATCH /api/notes/{noteId}/folder ==========

	@Nested
	@DisplayName("PATCH /api/notes/{noteId}/folder")
	class Move {

		@Test
		@DisplayName("folderId가 있으면 해당 폴더로 이동한다")
		void successWhenFolderIdIsProvided() throws Exception {
			// given
			Long userId = 1L;
			Long noteId = 10L;
			Long folderId = 20L;

			NoteDetailResult result = new NoteDetailResult(
					noteId,
					folderId,
					userId,
					"테스트 제목",
					"테스트 내용",
					NoteVisibility.PRIVATE,
					true,
					null,
					null
			);

			given(noteService.moveNote(any(NoteMoveCommand.class))).willReturn(result);

			SecurityContextHolder.getContext()
					.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));

			// when & then
			mockMvc.perform(patch("/api/notes/{noteId}/folder", noteId)
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"folderId": 20}
									"""))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.noteId").value(noteId))
					.andExpect(jsonPath("$.data.folderId").value(folderId))
					.andExpect(jsonPath("$.data.userId").value(userId));

			ArgumentCaptor<NoteMoveCommand> captor = ArgumentCaptor.forClass(NoteMoveCommand.class);
			verify(noteService).moveNote(captor.capture());
			assertThat(captor.getValue().noteId()).isEqualTo(noteId);
			assertThat(captor.getValue().userId()).isEqualTo(userId);
			assertThat(captor.getValue().targetFolderId()).isEqualTo(folderId);
		}

		@Test
		@DisplayName("folderId가 null이면 루트로 이동한다")
		void successWhenFolderIdIsNull() throws Exception {
			// given
			Long userId = 1L;
			Long noteId = 10L;

			NoteDetailResult result = new NoteDetailResult(
					noteId,
					null,
					userId,
					"테스트 제목",
					"테스트 내용",
					NoteVisibility.PRIVATE,
					true,
					null,
					null
			);

			given(noteService.moveNote(any(NoteMoveCommand.class))).willReturn(result);

			SecurityContextHolder.getContext()
					.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));

			// when & then
			mockMvc.perform(patch("/api/notes/{noteId}/folder", noteId)
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"folderId": null}
									"""))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.noteId").value(noteId))
					.andExpect(jsonPath("$.data.folderId").isEmpty());

			ArgumentCaptor<NoteMoveCommand> captor = ArgumentCaptor.forClass(NoteMoveCommand.class);
			verify(noteService).moveNote(captor.capture());
			assertThat(captor.getValue().targetFolderId()).isNull();
		}

		@Test
		@DisplayName("folderId 필드가 없으면 400 validation 에러를 반환한다")
		void returnsBadRequestWhenFolderIdIsMissing() throws Exception {
			// given
			Long userId = 1L;
			Long noteId = 10L;
			SecurityContextHolder.getContext()
					.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));

			// when & then
			mockMvc.perform(patch("/api/notes/{noteId}/folder", noteId)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{}"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
					.andExpect(jsonPath("$.error.fieldErrors[0].field").value("folderId"));
		}

		@Test
		@DisplayName("folderId가 0 이하이면 400 validation 에러를 반환한다")
		void returnsBadRequestWhenFolderIdIsNotPositive() throws Exception {
			// given
			Long userId = 1L;
			Long noteId = 10L;
			SecurityContextHolder.getContext()
					.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));

			// when & then
			mockMvc.perform(patch("/api/notes/{noteId}/folder", noteId)
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"folderId": 0}
									"""))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
					.andExpect(jsonPath("$.error.fieldErrors[0].field").value("folderId"));
		}

		@Test
		@DisplayName("없는 노트의 이동을 요청하면 찾을 수 없음을 응답한다")
		void returnsNotFoundWhenNoteDoesNotExist() throws Exception {
			// given
			Long userId = 1L;
			Long noteId = 10L;
			given(noteService.moveNote(any(NoteMoveCommand.class)))
					.willThrow(new BusinessException(ErrorCode.NOTE_NOT_FOUND));

			SecurityContextHolder.getContext()
					.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));

			// when & then
			mockMvc.perform(patch("/api/notes/{noteId}/folder", noteId)
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"folderId": 20}
									"""))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("NOTE_NOT_FOUND"));
		}

		@Test
		@DisplayName("다른 사용자가 노트 이동을 요청하면 권한 오류를 응답한다")
		void returnsForbiddenWhenRequesterIsNotAuthor() throws Exception {
			// given
			Long userId = 1L;
			Long noteId = 10L;
			given(noteService.moveNote(any(NoteMoveCommand.class)))
					.willThrow(new BusinessException(ErrorCode.NOTE_ACCESS_DENIED));

			SecurityContextHolder.getContext()
					.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));

			// when & then
			mockMvc.perform(patch("/api/notes/{noteId}/folder", noteId)
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"folderId": 20}
									"""))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("NOTE_ACCESS_DENIED"));
		}

		@Test
		@DisplayName("없는 폴더로 이동을 요청하면 찾을 수 없음을 응답한다")
		void returnsNotFoundWhenFolderDoesNotExist() throws Exception {
			// given
			Long userId = 1L;
			Long noteId = 10L;
			given(noteService.moveNote(any(NoteMoveCommand.class)))
					.willThrow(new BusinessException(ErrorCode.FOLDER_NOT_FOUND));

			SecurityContextHolder.getContext()
					.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));

			// when & then
			mockMvc.perform(patch("/api/notes/{noteId}/folder", noteId)
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"folderId": 20}
									"""))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("FOLDER_NOT_FOUND"));
		}

		@Test
		@DisplayName("다른 사용자의 폴더로 이동을 요청하면 권한 오류를 응답한다")
		void returnsForbiddenWhenFolderOwnedByAnotherUser() throws Exception {
			// given
			Long userId = 1L;
			Long noteId = 10L;
			given(noteService.moveNote(any(NoteMoveCommand.class)))
					.willThrow(new BusinessException(ErrorCode.FOLDER_ACCESS_DENIED));

			SecurityContextHolder.getContext()
					.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));

			// when & then
			mockMvc.perform(patch("/api/notes/{noteId}/folder", noteId)
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"folderId": 20}
									"""))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("FOLDER_ACCESS_DENIED"));
		}
	}

	@Nested
	@DisplayName("PATCH /api/notes/{noteId}/title")
	class Rename {

		@Test
		@DisplayName("노트 제목 변경 성공")
		void success() throws Exception {
			Long userId = 1L;
			Long noteId = 10L;
			NoteRenameRequest request = new NoteRenameRequest("새 제목");

			NoteDetailResult result = new NoteDetailResult(
					noteId,
					null,
					userId,
					"새 제목",
					"기존 내용",
					NoteVisibility.PRIVATE,
					false,
					null,
					null
			);

			given(noteService.renameNote(any(NoteRenameCommand.class)))
					.willReturn(result);

			SecurityContextHolder.getContext()
					.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));

			mockMvc.perform(patch("/api/notes/{noteId}/title", noteId)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.noteId").value(noteId))
					.andExpect(jsonPath("$.data.userId").value(userId))
					.andExpect(jsonPath("$.data.title").value("새 제목"));

			ArgumentCaptor<NoteRenameCommand> captor = ArgumentCaptor.forClass(NoteRenameCommand.class);
			verify(noteService).renameNote(captor.capture());
			assertThat(captor.getValue().noteId()).isEqualTo(noteId);
			assertThat(captor.getValue().userId()).isEqualTo(userId);
			assertThat(captor.getValue().title()).isEqualTo("새 제목");
		}

		@Test
		@DisplayName("제목을 공백으로 보내면 입력값 검증 오류를 응답한다")
		void returnsBadRequestWhenTitleIsBlank() throws Exception {
			Long userId = 1L;
			Long noteId = 10L;
			NoteRenameRequest request = new NoteRenameRequest(" ");

			SecurityContextHolder.getContext()
					.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));

			mockMvc.perform(patch("/api/notes/{noteId}/title", noteId)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
					.andExpect(jsonPath("$.error.fieldErrors[0].field").value("title"));
		}

		@Test
		@DisplayName("제목이 길이 제한을 넘으면 입력값 검증 오류를 응답한다")
		void returnsBadRequestWhenTitleIsTooLong() throws Exception {
			Long userId = 1L;
			Long noteId = 10L;
			NoteRenameRequest request = new NoteRenameRequest("a".repeat(201));

			SecurityContextHolder.getContext()
					.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));

			mockMvc.perform(patch("/api/notes/{noteId}/title", noteId)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
					.andExpect(jsonPath("$.error.fieldErrors[0].field").value("title"));
		}

		@Test
		@DisplayName("다른 사용자가 제목 변경을 요청하면 권한 오류를 응답한다")
		void returnsForbiddenWhenRequesterIsNotAuthor() throws Exception {
			Long userId = 1L;
			Long noteId = 10L;

			given(noteService.renameNote(any(NoteRenameCommand.class)))
					.willThrow(new BusinessException(ErrorCode.NOTE_ACCESS_DENIED));

			SecurityContextHolder.getContext()
					.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));

			mockMvc.perform(patch("/api/notes/{noteId}/title", noteId)
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"title":"남의 제목"}
									"""))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("NOTE_ACCESS_DENIED"));
		}

		@Test
		@DisplayName("없는 노트의 제목 변경을 요청하면 찾을 수 없음을 응답한다")
		void returnsNotFoundWhenNoteDoesNotExist() throws Exception {
			Long userId = 1L;
			Long noteId = 10L;

			given(noteService.renameNote(any(NoteRenameCommand.class)))
					.willThrow(new BusinessException(ErrorCode.NOTE_NOT_FOUND));

			SecurityContextHolder.getContext()
					.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));

			mockMvc.perform(patch("/api/notes/{noteId}/title", noteId)
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"title":"새 제목"}
									"""))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("NOTE_NOT_FOUND"));
		}
	}

	// ========== GET /api/notes/search ==========

	@Nested
	@DisplayName("GET /api/notes/search")
	class SearchPublic {

		@Test
		@DisplayName("공개 노트 검색 성공 시 200 OK 및 contents 반환")
		void success() throws Exception {
			Pageable pageable = PageRequest.of(0, 10);

			List<NoteSummaryResult> results = List.of(
					new NoteSummaryResult(1L, 10L, "spring 공개 제목", NoteVisibility.PUBLIC, true, null),
					new NoteSummaryResult(2L, null, "다른 공개 제목", NoteVisibility.PUBLIC, false, null)
			);

			Page<NoteSummaryResult> page = new PageImpl<>(results, pageable, results.size());

			given(noteService.searchPublicNotes(any(NoteSearchQuery.class)))
					.willReturn(page);

			mockMvc.perform(get("/api/notes/search")
							.param("keyword", "spring")
							.param("page", "0")
							.param("size", "10"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.contents").isArray())
					.andExpect(jsonPath("$.data.contents.length()").value(2))
					.andExpect(jsonPath("$.data.contents[0].title").value("spring 공개 제목"))
					.andExpect(jsonPath("$.data.contents[0].visibility").value("PUBLIC"))
					.andExpect(jsonPath("$.data.contents[1].folderId").isEmpty());

			ArgumentCaptor<NoteSearchQuery> captor = ArgumentCaptor.forClass(NoteSearchQuery.class);
			verify(noteService).searchPublicNotes(captor.capture());
			assertThat(captor.getValue().keyword()).isEqualTo("spring");
			assertThat(captor.getValue().pageable().getPageSize()).isEqualTo(10);
		}

		@Test
		@DisplayName("size가 MAX_PAGE_SIZE보다 크면 상한(100)으로 제한된다")
		void clampsPageSize() throws Exception {
			given(noteService.searchPublicNotes(any(NoteSearchQuery.class)))
					.willReturn(Page.empty());

			mockMvc.perform(get("/api/notes/search")
							.param("keyword", "spring")
							.param("page", "0")
							.param("size", "1000"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true));

			ArgumentCaptor<NoteSearchQuery> captor = ArgumentCaptor.forClass(NoteSearchQuery.class);
			verify(noteService).searchPublicNotes(captor.capture());
			assertThat(captor.getValue().keyword()).isEqualTo("spring");
			assertThat(captor.getValue().pageable().getPageSize()).isEqualTo(100);
		}

		@Test
		@DisplayName("검색어가 공백이면 공개 노트 검색 요청을 거절한다")
		void blankKeywordReturns400() throws Exception {
			given(noteService.searchPublicNotes(any(NoteSearchQuery.class)))
					.willThrow(new BusinessException(ErrorCode.NOTE_SEARCH_KEYWORD_REQUIRED));

			mockMvc.perform(get("/api/notes/search")
							.param("keyword", "   ")
							.param("page", "0")
							.param("size", "10"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("NOTE_SEARCH_KEYWORD_REQUIRED"));
		}
	}

	// ========== GET /api/notes/me/search ==========

	@Nested
	@DisplayName("GET /api/notes/me/search")
	class Search {

		@Test
		@DisplayName("검색 성공 시 200 OK 및 contents 반환")
		void success() throws Exception {
			// given
			Long userId = 1L;
			SecurityContextHolder.getContext()
					.setAuthentication(
							new UsernamePasswordAuthenticationToken(userId, null, List.of())
					);

			Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

			List<NoteSummaryResult> results = List.of(
					new NoteSummaryResult(1L, 10L, "spring 제목", NoteVisibility.PUBLIC, true, null),
					new NoteSummaryResult(2L, null, "기타 제목", NoteVisibility.PRIVATE, false, null)
			);

			Page<NoteSummaryResult> page = new PageImpl<>(results, pageable, results.size());

			given(noteService.searchMyNotes(eq(userId), any(NoteSearchQuery.class)))
					.willReturn(page);

			// when & then
			mockMvc.perform(get("/api/notes/me/search")
							.param("keyword", "spring")
							.param("page", "0")
							.param("size", "10"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.contents").isArray())
					.andExpect(jsonPath("$.data.contents.length()").value(2))
					.andExpect(jsonPath("$.data.contents[0].title").value("spring 제목"))
					.andExpect(jsonPath("$.data.contents[0].folderId").value(10L))
					.andExpect(jsonPath("$.data.contents[1].folderId").isEmpty());

			verify(noteService).searchMyNotes(eq(userId), any(NoteSearchQuery.class));
		}

		@Test
		@DisplayName("size가 MAX_PAGE_SIZE보다 크면 상한(100)으로 제한된다")
		void clampsPageSize() throws Exception {
			// given
			Long userId = 1L;
			SecurityContextHolder.getContext()
					.setAuthentication(
							new UsernamePasswordAuthenticationToken(userId, null, List.of())
					);

			given(noteService.searchMyNotes(eq(userId), any(NoteSearchQuery.class)))
					.willReturn(Page.empty());

			// when
			mockMvc.perform(get("/api/notes/me/search")
							.param("keyword", "spring")
							.param("page", "0")
							.param("size", "1000"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true));

			// then
			ArgumentCaptor<NoteSearchQuery> captor = ArgumentCaptor.forClass(NoteSearchQuery.class);
			verify(noteService).searchMyNotes(eq(userId), captor.capture());

			NoteSearchQuery passed = captor.getValue();
			assertThat(passed.keyword()).isEqualTo("spring");
			assertThat(passed.pageable()
					.getPageSize()).isEqualTo(100);
		}

		@Test
		@DisplayName("검색어가 공백이면 내 노트 검색 요청을 거절한다")
		void blankKeywordReturns400() throws Exception {
			// given
			Long userId = 1L;
			SecurityContextHolder.getContext()
					.setAuthentication(
							new UsernamePasswordAuthenticationToken(userId, null, List.of())
					);

			given(noteService.searchMyNotes(eq(userId), any(NoteSearchQuery.class)))
					.willThrow(new BusinessException(ErrorCode.NOTE_SEARCH_KEYWORD_REQUIRED));

			// when & then
			mockMvc.perform(get("/api/notes/me/search")
							.param("keyword", "   ")
							.param("page", "0")
							.param("size", "10"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("NOTE_SEARCH_KEYWORD_REQUIRED"));
		}

	}

	@Nested
	@DisplayName("GET /api/notes/me/search-slice")
	class SearchSlice {

		@Test
		@DisplayName("검색 성공 시 200 OK 및 hasNext를 반환")
		void success() throws Exception {
			// given
			Long userId = 1L;
			SecurityContextHolder.getContext()
					.setAuthentication(
							new UsernamePasswordAuthenticationToken(userId, null, List.of())
					);

			Pageable pageable = PageRequest.of(0, 10);

			List<NoteSummaryResult> results = List.of(
					new NoteSummaryResult(1L, 10L, "spring 제목", NoteVisibility.PUBLIC, true, null),
					new NoteSummaryResult(2L, null, "기타 제목", NoteVisibility.PRIVATE, false, null)
			);

			Slice<NoteSummaryResult> slice = new SliceImpl<>(results, pageable, true);

			given(noteService.searchMyNotesSlice(eq(userId), any(NoteSearchQuery.class)))
					.willReturn(slice);

			// when & then
			mockMvc.perform(get("/api/notes/me/search-slice")
							.param("keyword", "spring")
							.param("page", "0")
							.param("size", "10"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.contents").isArray())
					.andExpect(jsonPath("$.data.contents.length()").value(2))
					.andExpect(jsonPath("$.data.contents[0].title").value("spring 제목"))
					.andExpect(jsonPath("$.data.contents[0].folderId").value(10L))
					.andExpect(jsonPath("$.data.contents[1].folderId").isEmpty())
					.andExpect(jsonPath("$.data.hasNext").value(true));

			verify(noteService).searchMyNotesSlice(eq(userId), any(NoteSearchQuery.class));
		}

		@Test
		@DisplayName("size가 MAX_PAGE_SIZE보다 크면 상한(100)으로 제한된다")
		void clampsPageSize() throws Exception {
			// given
			Long userId = 1L;
			SecurityContextHolder.getContext()
					.setAuthentication(
							new UsernamePasswordAuthenticationToken(userId, null, List.of())
					);

			given(noteService.searchMyNotesSlice(eq(userId), any(NoteSearchQuery.class)))
					.willReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 100), false));

			// when
			mockMvc.perform(get("/api/notes/me/search-slice")
							.param("keyword", "spring")
							.param("page", "0")
							.param("size", "1000"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true));

			// then
			ArgumentCaptor<NoteSearchQuery> captor = ArgumentCaptor.forClass(NoteSearchQuery.class);
			verify(noteService).searchMyNotesSlice(eq(userId), captor.capture());

			NoteSearchQuery passed = captor.getValue();
			assertThat(passed.keyword()).isEqualTo("spring");
			assertThat(passed.pageable().getPageSize()).isEqualTo(100);
		}

		@Test
		@DisplayName("keyword가 공백이면 400 + NOTE_SEARCH_KEYWORD_REQUIRED 반환")
		void blankKeywordReturns400() throws Exception {
			// given
			Long userId = 1L;
			SecurityContextHolder.getContext()
					.setAuthentication(
							new UsernamePasswordAuthenticationToken(userId, null, List.of())
					);

			given(noteService.searchMyNotesSlice(eq(userId), any(NoteSearchQuery.class)))
					.willThrow(new BusinessException(ErrorCode.NOTE_SEARCH_KEYWORD_REQUIRED));

			// when & then
			mockMvc.perform(get("/api/notes/me/search-slice")
							.param("keyword", "   ")
							.param("page", "0")
							.param("size", "10"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("NOTE_SEARCH_KEYWORD_REQUIRED"));
		}
	}
}
