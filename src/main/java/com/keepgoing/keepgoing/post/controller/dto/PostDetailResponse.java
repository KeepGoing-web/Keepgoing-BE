package com.keepgoing.keepgoing.post.controller.dto;

import com.keepgoing.keepgoing.post.domain.PostVisibility;
import com.keepgoing.keepgoing.post.service.dto.PostDetailResult;

import java.time.LocalDateTime;

public record PostDetailResponse(
        Long postId,
        Long userId,
        String title,
        String content,
        PostVisibility visibility,
        boolean aiCollectable,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PostDetailResponse from(PostDetailResult result) {
        return new PostDetailResponse(
                result.postId(),
                result.userId(),
                result.title(),
                result.content(),
                result.visibility(),
                result.aiCollectable(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
