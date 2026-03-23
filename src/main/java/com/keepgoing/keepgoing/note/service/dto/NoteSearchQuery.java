package com.keepgoing.keepgoing.note.service.dto;

import org.springframework.data.domain.Pageable;

public record NoteSearchQuery(
        String keyword,
        Pageable pageable,
        NoteSearchMode mode
) {
    public NoteSearchQuery {
        if (keyword != null) {
            keyword = keyword.trim();
        }

        if (mode == null) {
            mode = NoteSearchMode.SCORE;
        }
    }

    // search LIKE 편의 생성자
    public NoteSearchQuery(String keyword, Pageable pageable) {
        this(keyword, pageable, null);
    }

    public boolean hasKeyword() {
        return keyword != null && !keyword.isBlank();
    }
}
