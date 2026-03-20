package com.keepgoing.keepgoing.note.service.dto;

import com.keepgoing.keepgoing.note.domain.NoteVisibility;

import java.time.LocalDateTime;

public record NoteSummaryResult(
        Long noteId,
        String title,
        NoteVisibility visibility,
        boolean aiCollectable,
        LocalDateTime createdAt
) {
}