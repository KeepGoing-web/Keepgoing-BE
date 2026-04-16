package com.keepgoing.keepgoing.folder.service;

import com.keepgoing.keepgoing.folder.domain.Folder;
import com.keepgoing.keepgoing.folder.repository.FolderRepository;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FolderLockService {

	private final FolderRepository folderRepository;

	@Transactional(propagation = Propagation.MANDATORY)
	public Folder lockActiveFolder(Long folderId) {
		return folderRepository.findForUpdate(folderId)
				.orElseThrow(() -> new BusinessException(ErrorCode.FOLDER_NOT_FOUND));
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public Map<Long, Folder> lockActiveFolders(Collection<Long> folderIds) {
		List<Long> ids = folderIds.stream()
				.filter(id -> id != null)
				.distinct()
				.sorted(Comparator.naturalOrder())
				.toList();
		if (ids.isEmpty()) {
			return Map.of();
		}

		List<Folder> folders = folderRepository.findAllForUpdate(ids);
		if (folders.size() != ids.size()) {
			throw new BusinessException(ErrorCode.FOLDER_NOT_FOUND);
		}

		Map<Long, Folder> result = new LinkedHashMap<>();
		for (Folder folder : folders) {
			result.put(folder.getId(), folder);
		}
		return result;
	}
}
