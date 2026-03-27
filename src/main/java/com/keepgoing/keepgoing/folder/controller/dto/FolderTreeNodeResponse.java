package com.keepgoing.keepgoing.folder.controller.dto;

import com.keepgoing.keepgoing.folder.service.dto.FolderTreeNodeResult;

import java.util.List;

public record FolderTreeNodeResponse(
        Long folderId,
        Long parentId,
        String name,
        List<FolderTreeNodeResponse> children
) {
	private static final int DEFAULT_MAX_DEPTH = 200;

	public static FolderTreeNodeResponse from(FolderTreeNodeResult result) {
		return from(result, DEFAULT_MAX_DEPTH);
	}

	public static FolderTreeNodeResponse from(FolderTreeNodeResult result, int maxDepth) {
		return from(result, maxDepth, 0);
	}

	private static FolderTreeNodeResponse from(FolderTreeNodeResult result, int maxDepth, int depth) {
		if (result == null) {
			return null;
		}
		if (maxDepth < 0) {
			throw new IllegalArgumentException("maxDepth must be >= 0");
		}
		if (depth >= maxDepth) {
			return new FolderTreeNodeResponse(
					result.folderId(),
					result.parentId(),
					result.name(),
					List.of()
			);
		}

		List<FolderTreeNodeResponse> children = (result.children() == null)
				? List.of()
				: result.children().stream()
						.map(child -> from(child, maxDepth, depth + 1))
						.toList();

		return new FolderTreeNodeResponse(
				result.folderId(),
				result.parentId(),
				result.name(),
				children
		);
	}
}
