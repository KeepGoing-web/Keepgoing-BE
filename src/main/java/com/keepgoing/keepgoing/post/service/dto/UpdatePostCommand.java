package com.keepgoing.keepgoing.post.service.dto;

import com.keepgoing.keepgoing.post.domain.PostVisibility;

/**
 * 포스트 수정에 필요한 서비스 계층 전용 커맨드
 * */
public record UpdatePostCommand(
        Long authorId,
        Long postId,
        String title,
        String content,
        PostVisibility visibility,
        boolean aiCollectable
) {
}
