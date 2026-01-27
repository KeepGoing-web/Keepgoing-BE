package com.keepgoing.keepgoing.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keepgoing.keepgoing.global.api.exception.GlobalExceptionHandler;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.post.controller.dto.PostCreateRequest;
import com.keepgoing.keepgoing.post.controller.dto.PostUpdateRequest;
import com.keepgoing.keepgoing.post.domain.PostVisibility;
import com.keepgoing.keepgoing.post.service.PostService;
import com.keepgoing.keepgoing.post.service.dto.PostCreateCommand;
import com.keepgoing.keepgoing.post.service.dto.PostDetailResult;
import com.keepgoing.keepgoing.post.service.dto.PostSummaryResult;
import com.keepgoing.keepgoing.post.service.dto.PostUpdateCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(PostController.class)
@Import(GlobalExceptionHandler.class)
public class PostControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    PostService postService;

    @MockitoBean
    JpaMetamodelMappingContext jpaMappingContext;

    // ========== Post ==========

    @Test
    @DisplayName("Post /api/posts - 글 생성 성공 시 201 Created 반환")
    void createPost_success() throws Exception {
        //given
        PostCreateRequest request = new PostCreateRequest(
                "테스트 제목",
                "테스트 내용",
                PostVisibility.PRIVATE,
                true
        );

        PostDetailResult result = new PostDetailResult(
                1L,
                1L,
                "테스트 제목",
                "테스트 내용",
                PostVisibility.PRIVATE,
                true,
                null,
                null
        );

        given(postService.createPost(any(PostCreateCommand.class)))
                .willReturn(result);

        String json = objectMapper.writeValueAsString(request);

        //when & then
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("테스트 제목"));
    }

    @Test
    @DisplayName("Post /api/posts - title이 비어있으면 400 에러")
    void createPost_failsWithEmptyTitle() throws Exception {
        //given
        PostCreateRequest request = new PostCreateRequest(
                "",
                "테스트 내용",
                PostVisibility.PRIVATE,
                true
        );

        String json = objectMapper.writeValueAsString(request);

        //when & then
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest()); // HTTP 400 Bad Request를 기대
    }

    // ========== GET ==========

    @Test
    @DisplayName("GET /api/posts/{postId} - 단일 게시글 조회 성공")
    void getPost_success() throws Exception {
        // given
        Long postId = 1L;

        PostDetailResult result = new PostDetailResult(
                1L,
                postId,
                "테스트 제목",
                "테스트 내용",
                PostVisibility.PRIVATE,
                true,
                null,
                null
        );

        given(postService.getPost(postId)).willReturn(result);

        // when & then
        mockMvc.perform(get("/api/posts/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("테스트 제목"));
    }

    @Test
    @DisplayName("GET /api/posts/{postId} - 없는 게시글이면 POST_NOT_FOUND 에러 응답")
    void getPost_notFound() throws Exception {
        // given
        Long postId = 999L;

        given(postService.getPost(postId))
                .willThrow(new BusinessException(ErrorCode.POST_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/posts/{postId}", postId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("POST_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/posts/me - 내 게시글 페이지네이션 조회 성공")
    void getPosts_success() throws Exception {
        // given
        Long userId = 1L; // TODO: Security 붙으면 현재 로그인 유저로 교체

        var posts = List.of(
                new PostSummaryResult(1L, "제목1", PostVisibility.PRIVATE, true, null),
                new PostSummaryResult(2L, "제목2", PostVisibility.PUBLIC, true, null)
        );

        // 페이지 정보 (0페이지, size=10, createdAt DESC 정렬)
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PostSummaryResult> postPage = new PageImpl<>(posts, pageable, posts.size());

        // 서비스 호출 스텁: userId + 어떤 Pageable 이 오든 postPage 반환
        given(postService.getPosts(eq(userId), any(Pageable.class)))
                .willReturn(postPage);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, List.of())
        );

        // when & then
        mockMvc.perform(get("/api/posts/me")
                        .param("page", "0")
                        .param("size", "1000"))
                .andExpect(status().isOk());

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/posts/me - size가 MAX_PAGE_SIZE보다 크면 상한으로 제한된다")
    void getPosts_clampsPageSize() throws Exception {
        // given
        Long userId = 1L;

        PostSummaryResult post = new PostSummaryResult(
                1L,
                "제목",
                PostVisibility.PUBLIC,
                true,
                null
        );

        int requestedSize = 1000;
        int expectedSize = 100; // MAX_PAGE_SIZE

        Pageable clampedPageable = PageRequest.of(0, expectedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PostSummaryResult> postPage = new PageImpl<>(List.of(post), clampedPageable, 1);

        given(postService.getPosts(eq(userId), any(Pageable.class)))
                .willReturn(postPage);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, List.of())
        );

        // when & then
        mockMvc.perform(get("/api/posts/me")
                        .param("page", "0")
                        .param("size", String.valueOf(requestedSize)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.size").value(expectedSize)); // 100

        SecurityContextHolder.clearContext();
    }

    // ========== PUT ==========

    @Test
    @DisplayName("PUT /api/posts/{postId} - 포스트 수정 성공")
    void updatePost_success() throws Exception {
        // given
        Long postId = 1L;
        Long userId = 1L; // TODO: Security 붙으면 현재 로그인 유저로 교체

        PostUpdateRequest request = new PostUpdateRequest(
                "수정된 제목",
                "수정된 내용",
                PostVisibility.PUBLIC,
                false
        );

        PostDetailResult result = new PostDetailResult(
                userId,
                postId,
                "수정된 제목",
                "수정된 내용",
                PostVisibility.PUBLIC,
                false,
                null,
                null
        );

        given(postService.updatePost(any(PostUpdateCommand.class)))
                .willReturn(result);

        String json = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(put("/api/posts/{postId}", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("수정된 제목"));
    }

    @Test
    @DisplayName("PUT /api/posts/{postId} - 작성자가 아니면 POST_ACCESS_DENIED 에러 반환")
    void updatePost_accessDenied() throws Exception {
        // given
        Long postId = 1L;

        PostUpdateRequest request = new PostUpdateRequest(
                "남의 글 수정",
                "이건 실패해야 함",
                PostVisibility.PUBLIC,
                true
        );

        // 서비스가 권한 체크 후 예외 던지는 상황 시뮬레이션
        given(postService.updatePost(any(PostUpdateCommand.class)))
                .willThrow(new BusinessException(ErrorCode.POST_ACCESS_DENIED));

        String json = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(put("/api/posts/{postId}", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden())                     // 403 가정
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("POST_ACCESS_DENIED"));
    }

    // ========== DELETE ==========

    @Test
    @DisplayName("DELETE /api/posts/{postId} - 포스트 삭제 성공")
    void deletePost_success() throws Exception {
        // given
        Long postId = 1L;
        Long userId = 1L; // TODO: Security 붙으면 현재 로그인 유저로 교체

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of())
        );

        willDoNothing().given(postService).deletePost(userId, postId);

        // when & then
        mockMvc.perform(delete("/api/posts/{postId}", postId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(postService).deletePost(userId, postId);

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("DELETE /api/posts/{postId} - 작성자가 아니면 POST_ACCESS_DENIED 에러 반환")
    void deletePost_accessDenied() throws Exception {
        // given
        Long postId = 1L;
        Long userId = 1L; // TODO: Security 붙으면 현재 로그인 유저로 교체

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of())
        );

        willThrow(new BusinessException(ErrorCode.POST_ACCESS_DENIED))
                .given(postService).deletePost(userId, postId);

        // when & then
        mockMvc.perform(delete("/api/posts/{postId}", postId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("POST_ACCESS_DENIED"));

        SecurityContextHolder.clearContext();
    }
}
