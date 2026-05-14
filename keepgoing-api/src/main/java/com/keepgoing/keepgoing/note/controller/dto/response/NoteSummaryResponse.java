package com.keepgoing.keepgoing.note.controller.dto.response;

import com.keepgoing.keepgoing.note.domain.NoteVisibility;
import com.keepgoing.keepgoing.note.service.dto.NoteSummaryResult;

import java.time.LocalDateTime;

public record NoteSummaryResponse(
        Long noteId,
        Long folderId,
        String title,
        NoteVisibility visibility,
        boolean aiCollectable,
        LocalDateTime createdAt
) {
    public static NoteSummaryResponse from(NoteSummaryResult result) {
        return new NoteSummaryResponse(
                result.noteId(),
                result.folderId(),
                result.title(),
                result.visibility(),
                result.aiCollectable(),
                result.createdAt()
        );
    }
}
