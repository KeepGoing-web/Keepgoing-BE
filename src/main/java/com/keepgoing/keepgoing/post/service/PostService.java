package com.keepgoing.keepgoing.post.service;

import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.post.domain.Post;
import com.keepgoing.keepgoing.post.domain.PostVisibility;
import com.keepgoing.keepgoing.post.dto.PostCreateRequest;
import com.keepgoing.keepgoing.post.dto.PostResponse;
import com.keepgoing.keepgoing.post.dto.PostUpdateRequest;
import com.keepgoing.keepgoing.post.repository.PostRepository;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    /**
     * 포스트 생성
     */
    public PostResponse createPost(Long authorId, PostCreateRequest request) {

        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Post post = Post.create(
                author,
                request.getTitle(),
                request.getContent(),
                request.getVisibility(),
                request.isAiCollectable()
        );

        Post saved = postRepository.save(post);
        return PostResponse.from(saved);
    }

    /**
     * 단일 포스트 조회
     */
    @Transactional(readOnly = true)
    public PostResponse getPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        return PostResponse.from(post);
    }

    /**
     * 내가 쓴 포스트 목록 조회
     */
    @Transactional(readOnly = true)
    public List<PostResponse> getMyPosts(Long authorId) {
        return postRepository.findByAuthor_IdOrderByCreatedAtDesc(authorId)
                .stream()
                .map(PostResponse::from)
                .toList();
    }

    /**
     * 포스트 수정
     */
    public PostResponse updatePost(Long authorId, Long postId, PostUpdateRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        post.validateAuthor(authorId);

        post.update(
                request.getTitle(),
                request.getContent(),
                request.getVisibility(),
                request.isAiCollectable()
        );

        return PostResponse.from(post);
    }

    /**
     * 포스트 삭제 (soft delete)
     */
    public void deletePost(Long authorId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        post.validateAuthor(authorId);

        post.softDelete(); // deleted_at만 채움 → @Where 때문에 이후 조회에서 빠짐
    }
}
