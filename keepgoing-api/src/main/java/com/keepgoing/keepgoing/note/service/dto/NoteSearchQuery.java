package com.keepgoing.keepgoing.note.service.dto;

import org.springframework.data.domain.Pageable;

public record NoteSearchQuery(
        String keyword,
        Pageable pageable
) {
    public NoteSearchQuery {
        if (keyword != null) {
            keyword = keyword.trim();
        }
    }

    public boolean hasKeyword() {
        return keyword != null && !keyword.isBlank();
    }
}
