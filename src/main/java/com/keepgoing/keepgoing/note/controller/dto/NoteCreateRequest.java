package com.keepgoing.keepgoing.note.controller.dto;

import com.keepgoing.keepgoing.note.domain.NoteVisibility;
import com.keepgoing.keepgoing.note.service.dto.NoteCreateCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoteCreateRequest(

        @NotBlank
        @Size(max = 200)
        String title,

        @NotBlank
        String content,

        NoteVisibility visibility,

        Boolean aiCollectable
) {
    public NoteCreateRequest {
        // visibility가 요청에 없거나 null이면 기본값 PRIVATE
        if (visibility == null) {
            visibility = NoteVisibility.PRIVATE;
        }
        // aiCollectable이 요청에 없거나 null이면 기본값 false
        if (aiCollectable == null) {
            aiCollectable = false;
        }
    }

    public NoteCreateCommand toCommand(Long userId) {
        return new NoteCreateCommand(
                userId,
                this.title,
                this.content,
                this.visibility,
                this.aiCollectable
        );
    }
}