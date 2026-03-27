package com.keepgoing.keepgoing.folder.service;

import com.keepgoing.keepgoing.folder.domain.Folder;
import com.keepgoing.keepgoing.folder.repository.FolderRepository;
import com.keepgoing.keepgoing.folder.service.dto.FolderCreateCommand;
import com.keepgoing.keepgoing.folder.service.dto.FolderSummaryResult;
import com.keepgoing.keepgoing.folder.service.dto.FolderTreeNodeResult;
import com.keepgoing.keepgoing.folder.service.dto.FolderTreeRow;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FolderService {

	private final UserRepository userRepository;
	private final FolderRepository folderRepository;

	@Transactional
	public FolderSummaryResult createFolder(FolderCreateCommand command) {
		User user = userRepository.findById(command.userId())
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		Folder parent = null;
		if (command.parentId() != null) {
			parent = folderRepository.findByIdAndDeletedAtIsNull(command.parentId())
					.orElseThrow(() -> new BusinessException(ErrorCode.FOLDER_NOT_FOUND));
			parent.validateOwner(user.getId());
		}

		boolean duplicated = (parent == null)
				? folderRepository.existsRootFolder(user.getId(), command.name())
				: folderRepository.existsChildFolder(user.getId(), parent.getId(), command.name());

		if (duplicated) {
			throw new BusinessException(ErrorCode.FOLDER_NAME_DUPLICATED);
		}

		Folder newFolder = Folder.create(user, parent, command.name());
		Folder saved = folderRepository.save(newFolder);

		return new FolderSummaryResult(
				saved.getId(),
				saved.getParent() != null ? saved.getParent().getId() : null,
				saved.getName());
	}

	@Transactional(readOnly = true)
	public List<FolderSummaryResult> getFolders(Long userId, Long parentId) {
		List<Folder> folders;
		if (parentId != null) {
			Folder parent = folderRepository.findByIdAndDeletedAtIsNull(parentId)
					.orElseThrow(() -> new BusinessException(ErrorCode.FOLDER_NOT_FOUND));

			parent.validateOwner(userId);

			folders = folderRepository.findChildFolders(userId, parentId);
		} else {
			folders = folderRepository.findRootFolders(userId);
		}

		return folders.stream()
				.map(f -> new FolderSummaryResult(
						f.getId(),
						f.getParent() != null ? f.getParent().getId() : null,
						f.getName()
				))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<FolderTreeNodeResult> getFolderTree(Long userId) {
		List<FolderTreeRow> rows = folderRepository.findTreeRows(userId);

		if (rows == null || rows.isEmpty()) {
			return List.of();
		}
		Map<Long, Node> nodes = new LinkedHashMap<>();
		for (FolderTreeRow r : rows) {
			nodes.put(r.folderId(), new Node(r.folderId(), r.parentId(), r.name()));
		}

		// 2) parentId 기반으로 children 연결
		List<Node> roots = new ArrayList<>();
		for (Node node : nodes.values()) {
			Long parentId = node.parentId;

			if (parentId == null) {
				roots.add(node);
				continue;
			}
			Node parent = nodes.get(parentId);

			// 부모가 soft delete 등으로 조회 대상에서 빠졌다면(데이터 불일치), 루트로 취급
			if (parent == null) {
				roots.add(node);
				continue;
			}

			parent.children.add(node);
		}

		roots.sort(Comparator.comparing(n -> n.name));
		List<FolderTreeNodeResult> results = roots.stream()
				.map(r -> toResult(r, new HashSet<>()))
				.toList();

		return List.copyOf(results);
	}

	private static class Node {
		final Long id;
		final Long parentId;
		final String name;
		final List<Node> children = new ArrayList<>();

		Node(Long id, Long parentId, String name) {
			this.id = id;
			this.parentId = parentId;
			this.name = name;
		}
	}


	private FolderTreeNodeResult toResult(Node node, Set<Long> visiting) {
		if (!visiting.add(node.id)) {
			return new FolderTreeNodeResult(node.id, node.parentId, node.name, List.of());
		}

		node.children.sort(Comparator.comparing(n -> n.name));
		List<FolderTreeNodeResult> children = node.children.stream()
				.map(c -> toResult(c, visiting))
				.toList();

		visiting.remove(node.id);
		return new FolderTreeNodeResult(node.id, node.parentId, node.name, List.copyOf(children));
	}
}
