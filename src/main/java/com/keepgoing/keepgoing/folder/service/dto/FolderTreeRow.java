package com.keepgoing.keepgoing.folder.service.dto;

public record FolderTreeRow(
        Long folderId,
        Long parentId,
        String name
) {
}
