package com.keepgoing.keepgoing.folder.controller.dto;

import com.keepgoing.keepgoing.folder.service.dto.FolderTreeNodeResult;

import java.util.List;

public record FolderTreeNodeResponse(
        Long folderId,
        Long parentId,
        String name,
        List<FolderTreeNodeResponse> children
) {
    public static FolderTreeNodeResponse from(FolderTreeNodeResult result) {
        return new FolderTreeNodeResponse(
                result.folderId(),
                result.parentId(),
                result.name(),
                result.children().stream().map(FolderTreeNodeResponse::from).toList()
        );
    }
}
