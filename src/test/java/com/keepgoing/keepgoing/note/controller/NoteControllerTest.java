package com.keepgoing.keepgoing.note.controller;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keepgoing.keepgoing.global.api.exception.GlobalExceptionHandler;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.global.security.jwt.JwtProvider;
import com.keepgoing.keepgoing.note.controller.dto.NoteCreateRequest;
import com.keepgoing.keepgoing.note.controller.dto.NoteUpdateRequest;
import com.keepgoing.keepgoing.note.domain.NoteVisibility;
import com.keepgoing.keepgoing.note.service.NoteService;
import com.keepgoing.keepgoing.note.service.dto.NoteCreateCommand;
import com.keepgoing.keepgoing.note.service.dto.NoteDetailResult;
import com.keepgoing.keepgoing.note.service.dto.NoteSearchQuery;
import com.keepgoing.keepgoing.note.service.dto.NoteSummaryResult;
import com.keepgoing.keepgoing.note.service.dto.NoteUpdateCommand;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(NoteController.class)
@Import(GlobalExceptionHandler.class)
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

    @Test
    @DisplayName("POST /api/notes - 글 생성 성공 시 201 Created 반환")
    void createNote_success() throws Exception {
        //given
        NoteCreateRequest request = new NoteCreateRequest(
                "테스트 제목",
                "테스트 내용",
                NoteVisibility.PRIVATE,
                true
        );

        NoteDetailResult result = new NoteDetailResult(
                1L,
                1L,
                "테스트 제목",
                "테스트 내용",
                NoteVisibility.PRIVATE,
                true,
                null,
                null
        );

        given(noteService.createNote(any(NoteCreateCommand.class)))
                .willReturn(result);

        String json = objectMapper.writeValueAsString(request);

        //when & then
        mockMvc.perform(post("/api/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("테스트 제목"));
    }

    @Test
    @DisplayName("POST /api/notes - title이 비어있으면 400 에러")
    void createNote_failsWithEmptyTitle() throws Exception {
        //given
        NoteCreateRequest request = new NoteCreateRequest(
                "",
                "테스트 내용",
                NoteVisibility.PRIVATE,
                true
        );

        String json = objectMapper.writeValueAsString(request);

        //when & then
        mockMvc.perform(post("/api/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest()); // HTTP 400 Bad Request를 기대
    }

    // ========== GET ==========

    @Test
    @DisplayName("GET /api/notes/{noteId} - 단일 게시글 조회 성공")
    void getNote_success() throws Exception {
        // given
        Long noteId = 1L;

        NoteDetailResult result = new NoteDetailResult(
                1L,
                noteId,
                "테스트 제목",
                "테스트 내용",
                NoteVisibility.PRIVATE,
                true,
                null,
                null
        );

        given(noteService.getNote(noteId)).willReturn(result);

        // when & then
        mockMvc.perform(get("/api/notes/{noteId}", noteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("테스트 제목"));
    }

    @Test
    @DisplayName("GET /api/notes/{noteId} - 없는 게시글이면 NOTE_NOT_FOUND 에러 응답")
    void getNote_notFound() throws Exception {
        // given
        Long noteId = 999L;

        given(noteService.getNote(noteId))
                .willThrow(new BusinessException(ErrorCode.NOTE_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/notes/{noteId}", noteId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOTE_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/notes/me - 내 게시글 페이지네이션 조회 성공")
    void getNotes_success() throws Exception {
        // given
        Long userId = 1L; // TODO: Security 붙으면 현재 로그인 유저로 교체

        var notes = List.of(
                new NoteSummaryResult(1L, "제목1", NoteVisibility.PRIVATE, true, null),
                new NoteSummaryResult(2L, "제목2", NoteVisibility.PUBLIC, true, null)
        );

        // 페이지 정보 (0페이지, size=10, createdAt DESC 정렬)
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NoteSummaryResult> notePage = new PageImpl<>(notes, pageable, notes.size());

        // 서비스 호출 스텁: userId + 어떤 Pageable 이 오든 notePage 반환
        given(noteService.getNotes(eq(userId), any(Pageable.class)))
                .willReturn(notePage);

        SecurityContextHolder.getContext().setAuthentication(
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
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.last").value(true));
        verify(noteService).getNotes(eq(userId), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/notes/me - size가 MAX_PAGE_SIZE보다 크면 상한으로 제한된다")
    void getNotes_clampsPageSize() throws Exception {
        // given
        Long userId = 1L;

        given(noteService.getNotes(eq(userId), any(Pageable.class)))
                .willReturn(Page.empty());

        SecurityContextHolder.getContext().setAuthentication(
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

        assertThat(captor.getValue().getPageSize()).isEqualTo(100);

    }

    // ========== PUT ==========

    @Test
    @DisplayName("PUT /api/notes/{noteId} - 노트 수정 성공")
    void updateNote_success() throws Exception {
        // given
        Long noteId = 1L;
        Long userId = 1L; // TODO: Security 붙으면 현재 로그인 유저로 교체

        NoteUpdateRequest request = new NoteUpdateRequest(
                "수정된 제목",
                "수정된 내용",
                NoteVisibility.PUBLIC,
                false
        );

        NoteDetailResult result = new NoteDetailResult(
                userId,
                noteId,
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
    @DisplayName("PUT /api/notes/{noteId} - 작성자가 아니면 NOTE_ACCESS_DENIED 에러 반환")
    void updateNote_accessDenied() throws Exception {
        // given
        Long noteId = 1L;

        NoteUpdateRequest request = new NoteUpdateRequest(
                "남의 글 수정",
                "이건 실패해야 함",
                NoteVisibility.PUBLIC,
                true
        );

        // 서비스가 권한 체크 후 예외 던지는 상황 시뮬레이션
        given(noteService.updateNote(any(NoteUpdateCommand.class)))
                .willThrow(new BusinessException(ErrorCode.NOTE_ACCESS_DENIED));

        String json = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(put("/api/notes/{noteId}", noteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden())                     // 403 가정
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOTE_ACCESS_DENIED"));
    }

    // ========== DELETE ==========

    @Test
    @DisplayName("DELETE /api/notes/{noteId} - 노트 삭제 성공")
    void deleteNote_success() throws Exception {
        // given
        Long noteId = 1L;
        Long userId = 1L; // TODO: Security 붙으면 현재 로그인 유저로 교체

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of())
        );

        willDoNothing().given(noteService).deleteNote(userId, noteId);

        // when & then
        mockMvc.perform(delete("/api/notes/{noteId}", noteId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(noteService).deleteNote(userId, noteId);

    }

    @Test
    @DisplayName("DELETE /api/notes/{noteId} - 작성자가 아니면 NOTE_ACCESS_DENIED 에러 반환")
    void deleteNote_accessDenied() throws Exception {
        // given
        Long noteId = 1L;
        Long userId = 1L; // TODO: Security 붙으면 현재 로그인 유저로 교체

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of())
        );

        willThrow(new BusinessException(ErrorCode.NOTE_ACCESS_DENIED))
                .given(noteService).deleteNote(userId, noteId);

        // when & then
        mockMvc.perform(delete("/api/notes/{noteId}", noteId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOTE_ACCESS_DENIED"));
    }

    // ========== SEARCH ==========

    @Test
    @DisplayName("GET /api/notes/me/search - 검색 성공 시 200 OK 및 contents 반환")
    void search_success() throws Exception {
        // given
        Long userId = 1L;
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of())
        );

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        List<NoteSummaryResult> results = List.of(
                new NoteSummaryResult(1L, "spring 제목", NoteVisibility.PUBLIC, true, null),
                new NoteSummaryResult(2L, "기타 제목", NoteVisibility.PRIVATE, false, null)
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
                .andExpect(jsonPath("$.data.contents[0].title").value("spring 제목"));

        verify(noteService).searchMyNotes(eq(userId), any(NoteSearchQuery.class));
    }

    @Test
    @DisplayName("GET /api/notes/me/search - size가 MAX_PAGE_SIZE보다 크면 상한(100)으로 제한된다")
    void search_clampsPageSize() throws Exception {
        // given
        Long userId = 1L;
        SecurityContextHolder.getContext().setAuthentication(
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
        assertThat(passed.pageable().getPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("GET /api/notes/me/search - keyword가 공백이면 400 + NOTE_SEARCH_KEYWORD_REQUIRED 반환")
    void search_blankKeyword_returns400() throws Exception {
        // given
        Long userId = 1L;
        SecurityContextHolder.getContext().setAuthentication(
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
