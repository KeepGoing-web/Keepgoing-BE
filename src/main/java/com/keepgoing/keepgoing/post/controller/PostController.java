package com.keepgoing.keepgoing.post.controller;

import com.keepgoing.keepgoing.global.api.response.ApiResponse;
import com.keepgoing.keepgoing.global.api.response.PagedResponse;
import com.keepgoing.keepgoing.post.controller.dto.PostCreateRequest;
import com.keepgoing.keepgoing.post.controller.dto.PostResponse;
import com.keepgoing.keepgoing.post.controller.dto.PostUpdateRequest;
import com.keepgoing.keepgoing.post.domain.Post;
import com.keepgoing.keepgoing.post.service.PostUseCase;
import com.keepgoing.keepgoing.post.service.dto.CreatePostCommand;
import com.keepgoing.keepgoing.post.service.dto.UpdatePostCommand;
import com.keepgoing.keepgoing.post.service.dto.UserPostQuery;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    private static final int MAX_PAGE_SIZE = 100;

    private final PostUseCase postUseCase;

    /**
     * 포스트 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @RequestBody @Valid PostCreateRequest request
    ) {
        Long authorId = 1L; // TODO: Security 붙으면 현재 로그인 유저로 교체

        CreatePostCommand command = new CreatePostCommand(
                authorId,
                request.getTitle(),
                request.getContent(),
                request.getVisibility(),
                request.isAiCollectable()
        );

        Post created = postUseCase.createPost(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(PostResponse.from(created)));
    }

    /**
     * 단일 포스트 조회
     */
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(
            @PathVariable Long postId
    ) {
        Post post = postUseCase.getPost(postId);

        return ResponseEntity.ok(ApiResponse.success(PostResponse.from(post)));
    }

    /**
     * 내가 쓴 포스트 목록 조회 (페이지네이션)
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PagedResponse<PostResponse>>> getMyPosts(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Long authorId = 1L; // TODO: Security 붙으면 현재 로그인 유저로 교체

        Pageable safePageable = PageRequest.of(
                Math.max(0, pageable.getPageNumber()),
                Math.min(pageable.getPageSize(), MAX_PAGE_SIZE),
                pageable.getSort()
        );

        UserPostQuery query = new UserPostQuery(authorId, safePageable);

        Page<Post> postPage = postUseCase.getMyPosts(query);

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
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @PathVariable Long postId,
            @RequestBody @Valid PostUpdateRequest request
    ) {
        Long authorId = 1L; // TODO: Security 붙으면 현재 로그인 유저로 교체

        UpdatePostCommand command = new UpdatePostCommand(
                authorId,
                postId,
                request.getTitle(),
                request.getContent(),
                request.getVisibility(),
                request.isAiCollectable()
        );

        Post updated = postUseCase.updatePost(command);

        return ResponseEntity.ok(ApiResponse.success(PostResponse.from(updated)));
    }

    /**
     * 포스트 삭제
     */
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId
    ) {
        Long authorId = 1L; // TODO: Security 붙으면 현재 로그인 유저로 교체

        postUseCase.deletePost(authorId, postId);

        return ResponseEntity.noContent().build();
    }
}
