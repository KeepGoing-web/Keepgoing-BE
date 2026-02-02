package com.keepgoing.keepgoing.post.service.dto;

import org.springframework.data.domain.Pageable;

public record PostSearchQuery(
        String keyword,
        Pageable pageable
) {
    public PostSearchQuery {
        if (keyword != null) {
            keyword = keyword.trim();
        }
    }

    public boolean hasKeyword() {
        return keyword != null && !keyword.isBlank();
    }
}
