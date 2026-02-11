package com.keepgoing.keepgoing.post.controller;

import com.keepgoing.keepgoing.global.api.response.ApiResponse;
import com.keepgoing.keepgoing.global.api.response.PagedResponse;
import com.keepgoing.keepgoing.post.controller.dto.PostCreateRequest;
import com.keepgoing.keepgoing.post.controller.dto.PostDetailResponse;
import com.keepgoing.keepgoing.post.controller.dto.PostSummaryResponse;
import com.keepgoing.keepgoing.post.controller.dto.PostUpdateRequest;
import com.keepgoing.keepgoing.post.service.PostService;
import com.keepgoing.keepgoing.post.service.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private static final int MAX_PAGE_SIZE = 100;

    private final PostService postService;

    /**
     * 포스트 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PostDetailResponse>> createPost(
            @RequestBody @Valid PostCreateRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        PostCreateCommand command = request.toCommand(userId);

        PostDetailResult created = postService.createPost(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(PostDetailResponse.from(created)));
    }

    /**
     * 단일 포스트 조회
     */
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> getPost(@PathVariable Long postId) {
        PostDetailResult post = postService.getPost(postId);

        return ResponseEntity.ok(ApiResponse.success(PostDetailResponse.from(post)));
    }

    /**
     * 포스트 목록 조회
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PagedResponse<PostSummaryResponse>>> getPosts(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            @AuthenticationPrincipal Long userId
    ) {
        Pageable safePageable = safePageable(pageable);

        Page<PostSummaryResult> page = postService.getPosts(userId, safePageable);

        List<PostSummaryResponse> contents = page.getContent().stream()
                .map(PostSummaryResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(page, contents)));
    }

    /**
     * 포스트 수정
     */
    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> updatePost(
            @PathVariable Long postId,
            @RequestBody @Valid PostUpdateRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        PostUpdateCommand command = request.toCommand(postId, userId);

        PostDetailResult updated = postService.updatePost(command);

        return ResponseEntity.ok(ApiResponse.success(PostDetailResponse.from(updated)));
    }

    /**
     * 포스트 삭제
     */
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId
    ) {
        postService.deletePost(userId, postId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 내 포스트 검색
     */
    @GetMapping("/me/search")
    public ResponseEntity<ApiResponse<PagedResponse<PostSummaryResponse>>> searchMyPosts(
            @RequestParam String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            @AuthenticationPrincipal Long userId
    ) {
        Pageable safePageable = safePageable(pageable);

        Page<PostSummaryResult> page = postService.searchMyPosts(
                userId,
                new PostSearchQuery(keyword, safePageable)
        );

        List<PostSummaryResponse> contents = page.getContent().stream()
               .map(PostSummaryResponse::from)
               .toList();

        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(page, contents)));
    }

    private Pageable safePageable(Pageable pageable) {
        int safeSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        return PageRequest.of(
                pageable.getPageNumber(),
                safeSize,
                pageable.getSort()
        );
    }
}
