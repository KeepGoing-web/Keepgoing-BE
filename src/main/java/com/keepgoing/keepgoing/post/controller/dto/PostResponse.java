package com.keepgoing.keepgoing.post.controller.dto;

import com.keepgoing.keepgoing.post.domain.Post;
import com.keepgoing.keepgoing.post.domain.PostVisibility;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PostResponse {

    private Long id;
    private Long authorId;
    private String title;
    private String content;
    private PostVisibility visibility;
    private boolean aiCollectable;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getAuthorId(),
                post.getTitle(),
                post.getContent(),
                post.getVisibility(),
                post.isAiCollectable(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
