package com.keepgoing.keepgoing.folder.service;

import com.keepgoing.keepgoing.folder.domain.Folder;
import com.keepgoing.keepgoing.folder.repository.FolderRepository;
import com.keepgoing.keepgoing.folder.service.dto.FolderCreateCommand;
import com.keepgoing.keepgoing.folder.service.dto.FolderSummaryResult;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
}
