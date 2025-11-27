package com.keepgoing.keepgoing.post.controller;

import com.keepgoing.keepgoing.global.api.response.ApiResponse;
import com.keepgoing.keepgoing.global.api.response.PagedResponse;
import com.keepgoing.keepgoing.post.controller.dto.PostCreateRequest;
import com.keepgoing.keepgoing.post.controller.dto.PostResponse;
import com.keepgoing.keepgoing.post.controller.dto.PostUpdateRequest;
import com.keepgoing.keepgoing.post.domain.Post;
import com.keepgoing.keepgoing.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * 포스트 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @RequestBody @Valid PostCreateRequest request
    ) {
        Long authorId = 1L; // TODO: Security 붙으면 현재 로그인 유저로 교체

        Post created = postService.createPost(
                authorId,
                request.getTitle(),
                request.getContent(),
                request.getVisibility(),
                request.isAiCollectable()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(PostResponse.from(created)));
    }

    /**
     * 단일 포스트 조회
     */
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(@PathVariable Long postId) {
        Post post = postService.getPost(postId);

        return ResponseEntity.ok(ApiResponse.success(PostResponse.from(post)));
    }

    /**
     * 내가 쓴 포스트 목록 조회
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PagedResponse<PostResponse>>> getMyPosts(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Long authorId = 1L; // TODO: Security 붙으면 현재 로그인 유저로 교체

        Page<Post> postPage = postService.getMyPosts(authorId, pageable);

        List<PostResponse> contents = postPage.getContent().stream()
                .map(PostResponse::from)
                .toList();

        PagedResponse<PostResponse> body = PagedResponse.of(postPage, contents);

        return ResponseEntity.ok(ApiResponse.success(body));
    }

    /**
     * 포스트 수정
     */
    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(@PathVariable Long postId,
                                                @RequestBody @Valid PostUpdateRequest request) {
        Long authorId = 1L; // TODO: Security 붙으면 현재 로그인 유저로 교체

        Post updated = postService.updatePost(
                authorId,
                postId,
                request.getTitle(),
                request.getContent(),
                request.getVisibility(),
                request.isAiCollectable()
        );

        return ResponseEntity.ok(ApiResponse.success(PostResponse.from(updated)));
    }

    /**
     * 포스트 삭제
     */
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId) {
        Long authorId = 1L; // TODO: Security 붙으면 현재 로그인 유저로 교체

        postService.deletePost(authorId, postId);

        return ResponseEntity.noContent().build();
    }
}
