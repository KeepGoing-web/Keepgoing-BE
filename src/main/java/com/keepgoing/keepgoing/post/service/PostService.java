package com.keepgoing.keepgoing.post.service;

import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.post.domain.Post;
import com.keepgoing.keepgoing.post.repository.PostRepository;
import com.keepgoing.keepgoing.post.service.dto.*;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    /**
     * 포스트 생성
     */
    public PostDetailResult createPost(PostCreateCommand command) {
        User author = userRepository.findById(command.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Post post = Post.create(
                author,
                command.title(),
                command.content(),
                command.visibility(),
                command.aiCollectable()
        );
        Post saved = postRepository.save(post);
        return toDetailResult(saved);
    }

    /**
     * 단일 포스트 조회
     */
    @Transactional(readOnly = true)
    public PostDetailResult getPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        return toDetailResult(post);
    }

    /**
     * 포스트 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<PostSummaryResult> getPosts(Long userId, Pageable pageable) {
        return postRepository.findByAuthor_Id(userId, pageable)
                .map(this::toSummaryResult);
    }

    /**
     * 포스트 수정
     */
    public PostDetailResult updatePost(PostUpdateCommand command) {
        Post post = postRepository.findById(command.postId())
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        post.validateAuthor(command.userId());

        post.update(
                command.title(),
                command.content(),
                command.visibility(),
                command.aiCollectable()
        );

        return toDetailResult(post);
    }

    /**
     * 포스트 삭제 (soft delete)
     */
    public void deletePost(Long userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        post.validateAuthor(userId);
        post.softDelete(); // deleted_at만 채움 → @Where 때문에 이후 조회에서 빠짐
    }

    /**
     * 포스트 검색
     */
    public Page<PostSummaryResult> searchPost(PostSearchQuery query) {
        if (query == null || !query.hasKeyword()) {
            throw new BusinessException(ErrorCode.POST_SEARCH_KEYWORD_REQUIRED);
        }

        Page<Post> page = postRepository.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(
                query.keyword(),
                query.keyword(),
                query.pageable()
        );

        return page.map(this::toSummaryResult);
    }

    private PostDetailResult toDetailResult(Post post) {
        return new PostDetailResult(
                post.getId(),
                post.getAuthor().getId(),
                post.getTitle(),
                post.getContent(),
                post.getVisibility(),
                post.isAiCollectable(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    private PostSummaryResult toSummaryResult(Post post) {
        return new PostSummaryResult(
                post.getId(),
                post.getTitle(),
                post.getVisibility(),
                post.isAiCollectable(),
                post.getCreatedAt()
        );
    }
}
