package com.keepgoing.keepgoing.post.service.dto;

import com.keepgoing.keepgoing.post.domain.PostVisibility;

public record PostUpdateCommand(
    Long postId,
    Long userId,
    String title,
    String content,
    PostVisibility visibility,
    boolean aiCollectable
) {}
