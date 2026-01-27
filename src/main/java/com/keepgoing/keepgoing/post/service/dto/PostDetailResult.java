package com.keepgoing.keepgoing.post.service.dto;

import com.keepgoing.keepgoing.post.domain.PostVisibility;

import java.time.LocalDateTime;

public record PostDetailResult(
    Long postId,
    Long authorId,
    String title,
    String content,
    PostVisibility visibility,
    boolean aiCollectable,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
