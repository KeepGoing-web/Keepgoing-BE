package com.keepgoing.keepgoing.post.service.dto;

import com.keepgoing.keepgoing.post.domain.PostVisibility;

import java.time.LocalDateTime;

public record PostSummaryResult(
        Long postId,
        String title,
        PostVisibility visibility,
        boolean aiCollectable,
        LocalDateTime createdAt
) {
}