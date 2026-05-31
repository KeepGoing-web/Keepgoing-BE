package com.keepgoing.keepgoing.note.controller.dto.response;

import com.keepgoing.keepgoing.note.domain.NoteVisibility;
import com.keepgoing.keepgoing.note.service.dto.NoteDetailResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "노트 상세 응답")
public record NoteDetailResponse(
        Long noteId,
        @Schema(
                description = "노트가 속한 폴더 ID. 루트 노트면 null입니다.",
                example = "10",
                nullable = true
        )
        Long folderId,
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
                result.folderId(),
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
