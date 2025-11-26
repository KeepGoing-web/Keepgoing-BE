package com.keepgoing.keepgoing.post.controller;

import com.keepgoing.keepgoing.global.api.response.ApiResponse;
import com.keepgoing.keepgoing.post.controller.dto.PostCreateRequest;
import com.keepgoing.keepgoing.post.controller.dto.PostResponse;
import com.keepgoing.keepgoing.post.controller.dto.PostUpdateRequest;
import com.keepgoing.keepgoing.post.domain.Post;
import com.keepgoing.keepgoing.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<ApiResponse<List<PostResponse>>> getMyPosts() {
        Long authorId = 1L; // TODO: Security 붙으면 현재 로그인 유저로 교체

        List<Post> posts = postService.getMyPosts(authorId);
        List<PostResponse> responses = posts.stream()
                .map(PostResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(responses));
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
