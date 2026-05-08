package com.keepgoing.keepgoing.note.controller.dto;

import com.keepgoing.keepgoing.note.domain.NoteVisibility;
import com.keepgoing.keepgoing.note.service.dto.NoteCreateCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "노트 생성 요청")
public record NoteCreateRequest(

        @Schema(
                description = "생성할 노트가 속할 폴더 ID. null이면 루트에 생성됩니다.",
                example = "10",
                nullable = true
        )
        Long folderId,

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
                this.folderId,
                this.title,
                this.content,
                this.visibility,
                this.aiCollectable
        );
    }
}
