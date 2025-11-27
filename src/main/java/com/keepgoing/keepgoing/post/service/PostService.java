package com.keepgoing.keepgoing.post.service;

import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.post.domain.Post;
import com.keepgoing.keepgoing.post.domain.PostVisibility;
import com.keepgoing.keepgoing.post.repository.PostRepository;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public Page<Post> getMyPosts(Long authorId, Pageable pageable) {
        return postRepository.findByAuthor_Id(authorId, pageable);
    }

    //  옛날 버전: 기존 컨트롤러/테스트가 쓰던 시그니처 (임시 어댑터)
    public List<Post> getMyPosts(Long authorId) {
        Page<Post> page = getMyPosts(
                authorId,
                PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return page.getContent();
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
