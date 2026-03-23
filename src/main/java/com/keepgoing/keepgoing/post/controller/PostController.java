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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
     * 내 포스트 검색 (LIKE baseline)
     * - FULLTEXT와 비교 측정을 위해 별도 endpoint로 유지
     * - 정렬/페이지는 Controller에서 safePageableUnsorted로 고정한다.
     * - 따라서 Repository 쿼리 자체에 ORDER BY(createdAt DESC)를 명시해 결과 정렬을 보장한다.
     * - keyword는 앞/뒤 공백을 제거(trim)하여 FULLTEXT와 입력 정규화 정책을 일치시킨다.
     */
    @GetMapping("/me/search-like")
    public ResponseEntity<ApiResponse<PagedResponse<PostSummaryResponse>>> searchMyPostsLike(
            @RequestParam String keyword,
            @PageableDefault(size = 10)
            Pageable pageable,
            @AuthenticationPrincipal Long userId
    ) {
        Pageable safePageable = safePageableUnsorted(pageable);

        Page<PostSummaryResult> page = postService.searchMyPostsLike(
                userId,
                new PostSearchQuery(keyword, safePageable)
        );

        List<PostSummaryResponse> contents = page.getContent().stream()
                .map(PostSummaryResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(page, contents)));
    }

    /**
     * 내 포스트 검색(FULLTEXT)
     */
    @GetMapping("/me/search")
    public ResponseEntity<ApiResponse<PagedResponse<PostSummaryResponse>>> searchMyPosts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "SCORE") PostSearchMode mode,
            @PageableDefault(size = 10)
            Pageable pageable,
            @AuthenticationPrincipal Long userId
    ) {
        Pageable safePageable = safePageableUnsorted(pageable);

        Page<PostSummaryResult> page = postService.searchMyPosts(
                userId,
                new PostSearchQuery(keyword, safePageable, mode)
        );

        List<PostSummaryResponse> contents = page.getContent().stream()
                .map(PostSummaryResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(page, contents)));
    }

    /**
     * 전체 포스트 검색
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedResponse<PostSummaryResponse>>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "SCORE") PostSearchMode mode,
            @PageableDefault(size = 10)
            Pageable pageable
    ) {
        Pageable safePageable = safePageableUnsorted(pageable);

        Page<PostSummaryResult> page = postService.searchPost(
                new PostSearchQuery(keyword, safePageable, mode)
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

    private Pageable safePageableUnsorted(Pageable pageable) {
        int safeSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        return PageRequest.of(
                pageable.getPageNumber(),
                safeSize
        );
    }
}
