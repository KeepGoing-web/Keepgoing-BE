package com.keepgoing.keepgoing.post.controller.dto;

import com.keepgoing.keepgoing.post.domain.PostVisibility;
import com.keepgoing.keepgoing.post.service.dto.PostSummaryResult;

import java.time.LocalDateTime;

public record PostSummaryResponse(
        Long postId,
        String title,
        PostVisibility visibility,
        boolean aiCollectable,
        LocalDateTime createdAt
) {
    public static PostSummaryResponse from(PostSummaryResult result) {
        return new PostSummaryResponse(
                result.postId(),
                result.title(),
                result.visibility(),
                result.aiCollectable(),
                result.createdAt()
        );
    }
}
