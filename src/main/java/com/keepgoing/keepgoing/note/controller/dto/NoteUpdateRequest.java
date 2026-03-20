package com.keepgoing.keepgoing.note.controller.dto;

import com.keepgoing.keepgoing.note.domain.NoteVisibility;
import com.keepgoing.keepgoing.note.service.dto.NoteUpdateCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NoteUpdateRequest(

    @NotBlank
    @Size(max = 200)
    String title,

    @NotBlank
    String content,

    @NotNull
    NoteVisibility visibility,

    @NotNull
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
