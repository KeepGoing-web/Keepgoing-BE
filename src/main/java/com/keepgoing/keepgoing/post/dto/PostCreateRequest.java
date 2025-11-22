package com.keepgoing.keepgoing.post.dto;

import com.keepgoing.keepgoing.post.domain.PostVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PostCreateRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    private String content;

    private PostVisibility visibility = PostVisibility.PRIVATE;

    private boolean aiCollectable = true;
}
