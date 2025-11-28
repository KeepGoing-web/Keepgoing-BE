package com.keepgoing.keepgoing.post.service;

import com.keepgoing.keepgoing.post.domain.Post;
import com.keepgoing.keepgoing.post.service.dto.CreatePostCommand;
import com.keepgoing.keepgoing.post.service.dto.UpdatePostCommand;
import com.keepgoing.keepgoing.post.service.dto.UserPostQuery;
import org.springframework.data.domain.Page;

/**
 * Post 도메인과 관련된 애플리케이션 서비스(유스케이스) 계약을 정의합니다.
 * <p>
 * 컨트롤러(Web 계층)는 이 인터페이스에만 의존하며,
 * 실제 구현은 {@link PostService} 등에서 담당합니다.
 */
public interface PostUseCase {

    /**
     * 새 게시글을 생성합니다.
     *
     * @param command 게시글 생성에 필요한 작성자 ID, 제목, 내용, 공개 범위, AI 수집 여부를 포함하는 명령 객체
     * @return 생성된 {@link Post} 엔티티
     * @throws com.keepgoing.keepgoing.global.common.error.BusinessException
     *         작성자를 찾을 수 없는 경우 ({@link com.keepgoing.keepgoing.global.common.error.ErrorCode#USER_NOT_FOUND})
     */
    Post createPost(CreatePostCommand command);

    /**
     * 게시글 ID로 단일 게시글을 조회합니다.
     *
     * @param postId 조회할 게시글 ID
     * @return 조회된 {@link Post} 엔티티
     * @throws com.keepgoing.keepgoing.global.common.error.BusinessException
     *         게시글을 찾을 수 없는 경우 ({@link com.keepgoing.keepgoing.global.common.error.ErrorCode#POST_NOT_FOUND})
     */
    Post getPost(Long postId);

    /**
     * 특정 사용자가 작성한 게시글을 페이지네이션 기준으로 조회합니다.
     *
     * @param query 작성자 ID와 페이지 정보가 포함된 조회 조건
     * @return 해당 사용자의 게시글 {@link Page}
     */
    Page<Post> getMyPosts(UserPostQuery query);

    /**
     * 기존 게시글을 수정합니다.
     *
     * @param command 게시글 수정에 필요한 작성자 ID, 게시글 ID, 수정할 내용이 포함된 명령 객체
     * @return 수정된 {@link Post} 엔티티
     * @throws com.keepgoing.keepgoing.global.common.error.BusinessException
     *         게시글이 존재하지 않거나 ({@link com.keepgoing.keepgoing.global.common.error.ErrorCode#POST_NOT_FOUND}),
     *         작성자가 아닌 사용자가 수정하려는 경우 ({@link com.keepgoing.keepgoing.global.common.error.ErrorCode#POST_ACCESS_DENIED})
     */
    Post updatePost(UpdatePostCommand command);

    /**
     * 게시글을 소프트 삭제(soft delete)합니다.
     *
     * @param authorId 삭제를 시도하는 사용자 ID
     * @param postId   삭제할 게시글 ID
     * @throws com.keepgoing.keepgoing.global.common.error.BusinessException
     *         게시글이 존재하지 않거나 ({@link com.keepgoing.keepgoing.global.common.error.ErrorCode#POST_NOT_FOUND}),
     *         작성자가 아닌 사용자가 삭제하려는 경우 ({@link com.keepgoing.keepgoing.global.common.error.ErrorCode#POST_ACCESS_DENIED})
     */
    void deletePost(Long authorId, Long postId);
}