package com.keepgoing.keepgoing.post.service;

import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.post.domain.Post;
import com.keepgoing.keepgoing.post.domain.PostVisibility;
import com.keepgoing.keepgoing.post.repository.PostRepository;
import com.keepgoing.keepgoing.post.service.dto.CreatePostCommand;
import com.keepgoing.keepgoing.post.service.dto.UpdatePostCommand;
import com.keepgoing.keepgoing.post.service.dto.UserPostQuery;
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
public class PostService implements PostUseCase{

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    /**
     * 포스트 생성
     */
    @Override
    public Post createPost(CreatePostCommand command) {
        User author = userRepository.findById(command.authorId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Post post = Post.create(
                author,
                command.title(),
                command.content(),
                command.visibility(),
                command.aiCollectable()
        );

        return postRepository.save(post);
    }

    /**
     * 단일 포스트 조회
     */
    @Override
    @Transactional(readOnly = true)
    public Post getPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    /**
     * 내가 쓴 포스트 목록 조회
     */
    @Override
    @Transactional(readOnly = true)
    public Page<Post> getMyPosts(UserPostQuery query) {
        return postRepository.findByAuthor_Id(query.authorId(), query.pageable());
    }

    /**
     * 포스트 수정
     * */
    @Override
    public Post updatePost(UpdatePostCommand command) {
        Post post = postRepository.findById(command.postId())
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        post.validateAuthor(command.authorId());

        post.update(
                command.title(),
                command.content(),
                command.visibility(),
                command.aiCollectable()
        );

        return post;
    }

    /**
     * 포스트 삭제 (soft delete)
     */
    @Override
    public void deletePost(Long authorId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        post.validateAuthor(authorId);
        post.softDelete();
    }
}
