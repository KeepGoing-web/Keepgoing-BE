package com.keepgoing.keepgoing.post.controller.dto;

import com.keepgoing.keepgoing.post.domain.PostVisibility;
import com.keepgoing.keepgoing.post.service.dto.PostUpdateCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostUpdateRequest (

    @NotBlank
    @Size(max = 200)
    String title,

    @NotBlank
    String content,

    @NotNull
    PostVisibility visibility,

    @NotNull
    Boolean aiCollectable
) {
    public PostUpdateCommand toCommand(Long postId, Long userId) {
        return new PostUpdateCommand(
                postId,
                userId,
                title,
                content,
                visibility,
                aiCollectable
        );
    }
}
