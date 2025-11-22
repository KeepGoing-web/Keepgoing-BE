package com.keepgoing.keepgoing.post.dto;

import com.keepgoing.keepgoing.post.domain.PostVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Getter
@NoArgsConstructor
public class PostUpdateRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    private String content;

    @NonNull
    private PostVisibility visibility;

    private boolean aiCollectable;
}
