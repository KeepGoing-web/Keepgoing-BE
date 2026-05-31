package com.keepgoing.keepgoing.folder.controller.dto;

import com.keepgoing.keepgoing.folder.service.dto.FolderSummaryResult;

public record FolderSummaryResponse(
        Long folderId,
        String name,
        Long parentId
) {
    public static FolderSummaryResponse from(FolderSummaryResult result) {
        return new FolderSummaryResponse(
                result.folderId(),
                result.name(),
                result.parentId()
        );
    }
}
