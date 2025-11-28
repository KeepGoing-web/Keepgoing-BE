package com.keepgoing.keepgoing.post.service.dto;

import com.keepgoing.keepgoing.post.domain.PostVisibility;

/**
 * 포스트 생성에 필요한 서비스 계층 전용 커맨드
 * */
public record CreatePostCommand(
        Long authorId,
        String title,
        String content,
        PostVisibility visibility,
        boolean aiCollectable
) {}

