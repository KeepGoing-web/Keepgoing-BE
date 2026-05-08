package com.keepgoing.keepgoing.note.controller.dto.request;

import com.keepgoing.keepgoing.note.domain.NoteVisibility;
import com.keepgoing.keepgoing.note.service.dto.NoteUpdateCommand;
import jakarta.validation.constraints.Size;

public record NoteUpdateRequest(

    @Size(max = 200)
    String title,

    String content,

    NoteVisibility visibility,

    Boolean aiCollectable
) {
    public NoteUpdateCommand toCommand(Long noteId, Long userId) {
        return new NoteUpdateCommand(
                noteId,
                userId,
                title,
                content,
                visibility,
                aiCollectable
        );
    }
}
