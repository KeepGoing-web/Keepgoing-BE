package com.keepgoing.keepgoing.post.controller;

import com.keepgoing.keepgoing.global.api.response.ApiResponse;
import com.keepgoing.keepgoing.post.dto.PostCreateRequest;
import com.keepgoing.keepgoing.post.dto.PostResponse;
import com.keepgoing.keepgoing.post.dto.PostUpdateRequest;
import com.keepgoing.keepgoing.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public ApiResponse<PostResponse> createPost(@RequestBody @Valid PostCreateRequest request) {
        Long authorId = 1L;
        return ApiResponse.success(postService.createPost(authorId, request));
    }

    /**
     * 내가 쓴 포스트 목록 조회
     */
    @GetMapping("/me")
    public ApiResponse<List<PostResponse>> getMyPosts() {
        Long authorId = 1L;
        return ApiResponse.success(postService.getMyPosts(authorId));
    }

    /**
     * 단일 포스트 조회
     */
    @GetMapping("/{postId}")
    public ApiResponse<PostResponse> getPost(@PathVariable Long postId) {
        return ApiResponse.success(postService.getPost(postId));
    }

    /**
     * 포스트 수정
     */
    @PutMapping("/{postId}")
    public ApiResponse<PostResponse> updatePost(@PathVariable Long postId,
                                                @RequestBody @Valid PostUpdateRequest request) {
        Long authorId = 1L;
        return ApiResponse.success(postService.updatePost(authorId, postId, request));
    }

    /**
     * 포스트 삭제
     */
    @DeleteMapping("/{postId}")
    public ApiResponse<Void> deletePost(@PathVariable Long postId) {
        Long authorId = 1L;
        postService.deletePost(authorId, postId);
        return ApiResponse.success(null);
    }
}
