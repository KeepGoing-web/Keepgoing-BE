package com.keepgoing.keepgoing.note.controller.dto;

import com.keepgoing.keepgoing.note.domain.NoteVisibility;
import com.keepgoing.keepgoing.note.service.dto.NoteDetailResult;

import java.time.LocalDateTime;

public record NoteDetailResponse(
        Long noteId,
        Long userId,
        String title,
        String content,
        NoteVisibility visibility,
        boolean aiCollectable,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static NoteDetailResponse from(NoteDetailResult result) {
        return new NoteDetailResponse(
                result.noteId(),
                result.userId(),
                result.title(),
                result.content(),
                result.visibility(),
                result.aiCollectable(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
