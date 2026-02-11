package com.keepgoing.keepgoing.post.controller.dto;

import com.keepgoing.keepgoing.post.domain.PostVisibility;
import com.keepgoing.keepgoing.post.service.dto.PostCreateCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostCreateRequest(

        @NotBlank
        @Size(max = 200)
        String title,

        @NotBlank
        String content,

        PostVisibility visibility,

        Boolean aiCollectable
) {
    public PostCreateRequest {
        // visibility가 요청에 없거나 null이면 기본값 PRIVATE
        if (visibility == null) {
            visibility = PostVisibility.PRIVATE;
        }
        // aiCollectable이 요청에 없거나 null이면 기본값 false
        if (aiCollectable == null) {
            aiCollectable = false;
        }
    }

    public PostCreateCommand toCommand(Long userId) {
        return new PostCreateCommand(
                userId,
                this.title,
                this.content,
                this.visibility,
                this.aiCollectable
        );
    }
}