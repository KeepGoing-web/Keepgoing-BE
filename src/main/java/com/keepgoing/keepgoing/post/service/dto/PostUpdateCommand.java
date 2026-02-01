package com.keepgoing.keepgoing.post.service.dto;

import com.keepgoing.keepgoing.post.domain.PostVisibility;

/**
 * 포스트 수정에 필요한 서비스 계층 전용 커맨드입니다.
 *
 * @param postId        수정할 포스트 ID
 * @param userId      수정하려는 사용자 ID
 * @param title         수정할 제목
 * @param content       수정할 본문 내용
 * @param visibility    수정 후 공개 범위
 * @param aiCollectable 수정 후 AI 수집 가능 여부
 */
public record PostUpdateCommand(
    Long postId,
    Long userId,
    String title,
    String content,
    PostVisibility visibility,
    boolean aiCollectable
) {}
