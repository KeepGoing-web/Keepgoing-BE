package com.keepgoing.keepgoing.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keepgoing.keepgoing.global.api.exception.GlobalExceptionHandler;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.post.controller.PostController;
import com.keepgoing.keepgoing.post.domain.Post;
import com.keepgoing.keepgoing.post.domain.PostVisibility;
import com.keepgoing.keepgoing.post.dto.PostCreateRequest;
import com.keepgoing.keepgoing.post.dto.PostResponse;
import com.keepgoing.keepgoing.post.dto.PostUpdateRequest;
import com.keepgoing.keepgoing.post.service.PostService;
import com.keepgoing.keepgoing.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PostController.class)
@Import(GlobalExceptionHandler.class)
public class PostControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    PostService postService;

    @MockBean
    JpaMetamodelMappingContext jpaMappingContext;

    // ========== Post ==========

    @Test
    @DisplayName("Post /api/v1/posts - 글 생성 성공")
    void createPost_success() throws Exception {
        //given
        PostCreateRequest request = PostCreateRequest.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .visibility(PostVisibility.PRIVATE)
                .aiCollectable(true)
                .build();

        User author = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("테스트유저")
                .build();

        Post post = Post.create(
                author,
                "테스트 제목",
                "테스트 내용",
                PostVisibility.PRIVATE,
                true
        );

        PostResponse response = PostResponse.from(post);

        given(postService.createPost(any(Long.class), any(PostCreateRequest.class)))
                .willReturn(response);

        String json = objectMapper.writeValueAsString(request);

        //when & then
        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("테스트 제목"));
    }

    @Test
    @DisplayName("Post /api/v1/posts - title이 비어있으면 400 에러")
    void createPost_failsWithEmptyTitle() throws Exception {
        //given
        PostCreateRequest request = PostCreateRequest.builder()
                .title("") // 유효성 검증에 실패할 값
                .content("테스트 내용")
                .build();

        String json = objectMapper.writeValueAsString(request);

        //when & then
        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest()); // HTTP 400 Bad Request를 기대
    }

    // ========== GET ==========

    @Test
    @DisplayName("GET /api/v1/posts/{postId} - 단일 게시글 조회 성공")
    void getPost_success() throws Exception {
        // given
        Long postId = 1L;

        User author = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("테스트유저")
                .build();

        Post post = Post.create(
                author,
                "테스트 제목",
                "테스트 내용",
                PostVisibility.PRIVATE,
                true
        );

        PostResponse response = PostResponse.from(post);

        given(postService.getPost(postId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/posts/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.data.id").value(postId))
                .andExpect(jsonPath("$.data.title").value("테스트 제목"));
    }

    @Test
    @DisplayName("GET /api/v1/posts/{postId} - 없는 게시글이면 POST_NOT_FOUND 에러 응답")
    void getPost_notFound() throws Exception {
        // given
        Long postId = 999L;

        given(postService.getPost(postId))
                .willThrow(new BusinessException(ErrorCode.POST_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/posts/{postId}", postId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("POST_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/v1/posts/me - 내 게시글 목록 조회 성공")
    void getMyPosts_success() throws Exception {
        // given
        Long authorId = 1L; // 컨트롤러 안에서 하드코딩된 값

        User author = User.builder()
                .id(authorId)
                .email("test@example.com")
                .name("테스트유저")
                .build();

        Post post1 = Post.create(author, "제목1", "내용1", PostVisibility.PRIVATE, true);
        Post post2 = Post.create(author, "제목2", "내용2", PostVisibility.PUBLIC, true);

        List<PostResponse> responses = List.of(
                PostResponse.from(post1),
                PostResponse.from(post2)
        );

        given(postService.getMyPosts(authorId)).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/v1/posts/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].title").value("제목1"))
                .andExpect(jsonPath("$.data[1].title").value("제목2"));
    }

    // ========== PUT ==========

    @Test
    @DisplayName("PUT /api/v1/posts/{postId} - 포스트 수정 성공")
    void updatePost_success() throws Exception {
        // given
        Long postId = 1L;
        Long authorId = 1L; // 컨트롤러에서 하드코딩한 값

        PostUpdateRequest request = PostUpdateRequest.builder()
                .title("수정된 제목")
                .content("수정된 내용")
                .visibility(PostVisibility.PUBLIC)
                .aiCollectable(false)
                .build();

        User author = User.builder()
                .id(authorId)
                .email("test@example.com")
                .name("테스트유저")
                .build();

        Post updatedPost = Post.create(
                author,
                "수정된 제목",
                "수정된 내용",
                PostVisibility.PUBLIC,
                false
        );
        PostResponse response = PostResponse.from(updatedPost);

        given(postService.updatePost(any(Long.class), eq(postId), any(PostUpdateRequest.class)))
                .willReturn(response);

        String json = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(put("/api/v1/posts/{postId}", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("수정된 제목"));
    }

    @Test
    @DisplayName("PUT /api/v1/posts/{postId} - 작성자가 아니면 POST_ACCESS_DENIED 에러 반환")
    void updatePost_accessDenied() throws Exception {
        // given
        Long postId = 1L;
        Long authorId = 1L; // 컨트롤러 안에서 하드코딩 쓰는 값

        PostUpdateRequest request = PostUpdateRequest.builder()
                .title("남의 글 수정")
                .content("이건 실패해야 함")
                .visibility(PostVisibility.PUBLIC)
                .aiCollectable(true)
                .build();

        // 서비스가 권한 체크 후 예외 던지는 상황 시뮬레이션
        given(postService.updatePost(eq(authorId), eq(postId), any(PostUpdateRequest.class)))
                .willThrow(new BusinessException(ErrorCode.POST_ACCESS_DENIED));

        String json = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(put("/api/v1/posts/{postId}", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden())                     // 403 가정
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("POST_ACCESS_DENIED"));
    }

    // ========== DELETE ==========

    @Test
    @DisplayName("DELETE /api/v1/posts/{postId} - 포스트 삭제 성공")
    void deletePost_success() throws Exception {
        // given
        Long postId = 1L;
        Long authorId = 1L;

        willDoNothing().given(postService).deletePost(authorId, postId);

        // when & then
        mockMvc.perform(delete("/api/v1/posts/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(postService).deletePost(authorId, postId);
    }

    @Test
    @DisplayName("DELETE /api/v1/posts/{postId} - 작성자가 아니면 POST_ACCESS_DENIED 에러 반환")
    void deletePost_accessDenied() throws Exception {
        // given
        Long postId = 1L;
        Long authorId = 1L; // 컨트롤러 내부에서 쓰는 값

        willThrow(new BusinessException(ErrorCode.POST_ACCESS_DENIED))
                .given(postService).deletePost(authorId, postId);

        // when & then
        mockMvc.perform(delete("/api/v1/posts/{postId}", postId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("POST_ACCESS_DENIED"));
    }
}
