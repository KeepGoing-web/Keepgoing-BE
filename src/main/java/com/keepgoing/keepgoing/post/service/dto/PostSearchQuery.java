package com.keepgoing.keepgoing.post.service.dto;

import org.springframework.data.domain.Pageable;

public record PostSearchQuery(
        String keyword,
        Pageable pageable,
        PostSearchMode mode
) {
    public PostSearchQuery {
        if (keyword != null) {
            keyword = keyword.trim();
        }

        if (mode == null) {
            mode = PostSearchMode.SCORE;
        }
    }

    // search LIKE 편의 생성자
    public PostSearchQuery(String keyword, Pageable pageable) {
        this(keyword, pageable, null);
    }

    public boolean hasKeyword() {
        return keyword != null && !keyword.isBlank();
    }
}
