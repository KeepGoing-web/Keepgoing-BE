package com.keepgoing.keepgoing.post.service.dto;

import com.keepgoing.keepgoing.post.domain.PostVisibility;

/**
 * 포스트 생성에 필요한 서비스 계층 전용 커맨드입니다.
 *
 * @param userId   작성자 ID
 * @param title      포스트 제목
 * @param content    포스트 본문 내용
 * @param visibility 공개 범위
 * @param aiCollectable AI 학습/수집에 활용 가능한지 여부
 */
public record PostCreateCommand(
    Long userId,
    String title,
    String content,
    PostVisibility visibility,
    boolean aiCollectable
) {}
