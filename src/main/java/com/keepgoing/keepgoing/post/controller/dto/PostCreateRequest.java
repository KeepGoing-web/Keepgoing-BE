package com.keepgoing.keepgoing.post.controller.dto;

import com.keepgoing.keepgoing.post.domain.PostVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PostCreateRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    private String content;

    @Builder.Default
    private PostVisibility visibility = PostVisibility.PRIVATE;

    @Builder.Default
    private boolean aiCollectable = true;
}
