package com.keepgoing.keepgoing.folder.service.dto;

import java.util.List;

public record FolderTreeNodeResult (
        Long folderId,
        Long parentId,
        String name,
        List<FolderTreeNodeResult> children
) {
}
