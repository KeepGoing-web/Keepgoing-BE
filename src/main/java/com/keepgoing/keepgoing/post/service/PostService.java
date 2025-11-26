package com.keepgoing.keepgoing.post.service;

import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.post.domain.Post;
import com.keepgoing.keepgoing.post.domain.PostVisibility;
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
    public Post createPost(Long authorId,
                           String title,
                           String content,
                           PostVisibility visibility,
                           boolean aiCollectable) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Post post = Post.create(
                author,
                title,
                content,
                visibility,
                aiCollectable
        );

        return postRepository.save(post);
    }

    /**
     * 단일 포스트 조회
     */
    @Transactional(readOnly = true)
    public Post getPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    /**
     * 내가 쓴 포스트 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Post> getMyPosts(Long authorId) {
        return postRepository.findByAuthor_IdOrderByCreatedAtDesc(authorId);
    }

    /**
     * 포스트 수정
     */
    public Post updatePost(Long authorId,
                           Long postId,
                           String title,
                           String content,
                           PostVisibility visibility,
                           boolean aiCollectable) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        post.validateAuthor(authorId);

        post.update(
                title,
                content,
                visibility,
                aiCollectable
        );

        return post;
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
