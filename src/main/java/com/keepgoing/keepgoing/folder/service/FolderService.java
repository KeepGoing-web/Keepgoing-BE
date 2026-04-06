package com.keepgoing.keepgoing.folder.service;

import com.keepgoing.keepgoing.folder.domain.Folder;
import com.keepgoing.keepgoing.folder.repository.FolderRepository;
import com.keepgoing.keepgoing.folder.repository.dto.FolderTreeRow;
import com.keepgoing.keepgoing.folder.service.dto.FolderCreateCommand;
import com.keepgoing.keepgoing.folder.service.dto.FolderRenameCommand;
import com.keepgoing.keepgoing.folder.service.dto.FolderSummaryResult;
import com.keepgoing.keepgoing.folder.service.dto.FolderTreeNodeResult;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.note.repository.NoteRepository;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FolderService {

	private final UserRepository userRepository;
	private final FolderRepository folderRepository;
	private final NoteRepository noteRepository;
	private static final int MAX_TREE_DEPTH = 200;

	@Transactional
	public FolderSummaryResult createFolder(FolderCreateCommand command) {
		Long userId = command.userId();
		Long parentId = command.parentId();
		String folderName = normalizeName(command.name());

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		if (folderName.isBlank()) {
			throw new BusinessException(ErrorCode.FOLDER_INVALID_NAME);
		}

		Folder parent = null;
		if (parentId != null) {
			parent = folderRepository.findByIdAndDeletedAtIsNull(parentId)
					.orElseThrow(() -> new BusinessException(ErrorCode.FOLDER_NOT_FOUND));
		}

		Folder newFolder = Folder.create(user, parent, folderName);
		if (isDuplicatedFolderName(userId, parent, folderName)) {
			throw new BusinessException(ErrorCode.FOLDER_NAME_DUPLICATED);
		}

		Folder saved = folderRepository.save(newFolder);
		return FolderSummaryResult.from(saved);
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
				.map(FolderSummaryResult::from)
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
		AtomicBoolean truncated = new AtomicBoolean(false);
		List<FolderTreeNodeResult> results = roots.stream()
				.map(r -> toResult(r, new HashSet<>(), 0, truncated))
				.toList();

		if (truncated.get()) {
			log.warn("folder tree truncated: userId={}, maxDepth={} (children cut)", userId, MAX_TREE_DEPTH);
		}

		return List.copyOf(results);
	}

	@Transactional
	public FolderSummaryResult renameFolder(FolderRenameCommand command) {
		Folder folder = folderRepository.findByIdAndDeletedAtIsNull(command.folderId())
				.orElseThrow(() -> new BusinessException(ErrorCode.FOLDER_NOT_FOUND));

		folder.validateOwner(command.userId());

		String newName = normalizeName(command.name());
		if (newName.isBlank()) {
			throw new BusinessException(ErrorCode.FOLDER_INVALID_NAME);
		}

		if (newName.equals(folder.getName())) {
			return FolderSummaryResult.from(folder);
		}

		Long userId = command.userId();
		Folder parent = folder.getParent();

		if (isDuplicatedFolderName(userId, parent, newName)) {
			throw new BusinessException(ErrorCode.FOLDER_NAME_DUPLICATED);
		}

		folder.rename(newName);

		return new FolderSummaryResult(
				folder.getId(),
				parent != null ? parent.getId() : null,
				folder.getName()
		);
	}

	@Transactional
	public void deleteFolder(Long userId, Long folderId) {
		Folder folder = folderRepository.findByIdAndDeletedAtIsNull(folderId)
				.orElseThrow(() -> new BusinessException(ErrorCode.FOLDER_NOT_FOUND));

		folder.validateOwner(userId);

		boolean hasChildren = folderRepository.existsByOwner_IdAndParent_IdAndDeletedAtIsNull(userId, folderId);
		if (hasChildren) {
			throw new BusinessException(ErrorCode.FOLDER_NOT_EMPTY);
		}

		boolean hasNotes = noteRepository.existsByFolder_IdAndDeletedAtIsNull(folderId);
		if (hasNotes) {
			throw new BusinessException(ErrorCode.FOLDER_NOT_EMPTY);
		}

		folder.softDelete();
	}

	private String normalizeName(String raw) {
		return raw == null ? "" : raw.trim();
	}

	private boolean isDuplicatedFolderName(Long userId, Folder parent, String folderName) {
		return (parent == null)
				? folderRepository.existsRootFolder(userId, folderName)
				: folderRepository.existsChildFolder(userId, parent.getId(), folderName);
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

	private FolderTreeNodeResult toResult(Node node, Set<Long> visiting, int depth, AtomicBoolean truncated) {
		// depth guard
		if (depth >= MAX_TREE_DEPTH) {
			truncated.set(true);
			return new FolderTreeNodeResult(node.id, node.parentId, node.name, List.of());
		}

		// cycle guard
		if (!visiting.add(node.id)) {
			truncated.set(true);
			log.warn("folder tree cycle detected: nodeId={} (children cut)", node.id);
			return new FolderTreeNodeResult(node.id, node.parentId, node.name, List.of());
		}

		node.children.sort(Comparator.comparing(n -> n.name));
		List<FolderTreeNodeResult> children = node.children.stream()
				.map(c -> toResult(c, visiting, depth + 1, truncated))
				.toList();

		visiting.remove(node.id);
		return new FolderTreeNodeResult(node.id, node.parentId, node.name, List.copyOf(children));
	}
}
