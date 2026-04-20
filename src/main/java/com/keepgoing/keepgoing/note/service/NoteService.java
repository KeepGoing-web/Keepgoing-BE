package com.keepgoing.keepgoing.note.service;

import com.keepgoing.keepgoing.activity.service.ActivityEventRecord;
import com.keepgoing.keepgoing.folder.domain.Folder;
import com.keepgoing.keepgoing.folder.repository.FolderRepository;
import com.keepgoing.keepgoing.folder.service.FolderLockService;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.note.domain.Note;
import com.keepgoing.keepgoing.note.domain.NoteVisibility;
import com.keepgoing.keepgoing.note.repository.NoteRepository;
import com.keepgoing.keepgoing.note.service.dto.NoteCreateCommand;
import com.keepgoing.keepgoing.note.service.dto.NoteDetailResult;
import com.keepgoing.keepgoing.note.service.dto.NoteMoveCommand;
import com.keepgoing.keepgoing.note.service.dto.NoteRenameCommand;
import com.keepgoing.keepgoing.note.service.dto.NoteSearchQuery;
import com.keepgoing.keepgoing.note.service.dto.NoteSummaryResult;
import com.keepgoing.keepgoing.note.service.dto.NoteUpdateCommand;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import java.util.Arrays;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NoteService {

	private final NoteRepository noteRepository;
	private final UserRepository userRepository;
	private final FolderRepository folderRepository;
	private final FolderLockService folderLockService;
	private final ActivityEventRecord activityEventRecord;

	/**
	 * 노트 생성
	 */
	@Transactional
	public NoteDetailResult createNote(NoteCreateCommand command) {
		User author = userRepository.findById(command.userId())
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		Folder folder = null;
		if (command.folderId() != null) {
			folder = folderLockService.lockActiveFolder(command.folderId());
		}

		Note note = Note.create(
				author,
				folder,
				command.title(),
				command.content(),
				command.visibility(),
				command.aiCollectable()
		);
		Note saved = noteRepository.save(note);
		activityEventRecord.recordNoteCreated(author, note);
		return NoteDetailResult.from(saved);
	}

	/**
	 * 단일 노트 조회
	 */
	@Transactional(readOnly = true)
	public NoteDetailResult getNote(Long viewerId, Long noteId) {
		Note note = noteRepository.findById(noteId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));

		ensureReadable(note, viewerId);

		return NoteDetailResult.from(note);
	}

	/**
	 * 노트 목록 조회
	 */
	@Transactional(readOnly = true)
	public Page<NoteSummaryResult> getNotes(Long userId, Pageable pageable) {
		return noteRepository.findByAuthor_Id(userId, pageable)
				.map(NoteSummaryResult::from);
	}

	/**
	 * 노트 수정
	 */
	@Transactional
	public NoteDetailResult updateNote(NoteUpdateCommand command) {
		Note note = noteRepository.findById(command.noteId())
				.orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));

		note.validateAuthor(command.userId());

		note.update(
				command.title(),
				command.content(),
				command.visibility(),
				command.aiCollectable()
		);

		activityEventRecord.recordNoteUpdated(note.getAuthor(), note);
		return NoteDetailResult.from(note);
	}

	/**
	 * 노트 삭제 (soft delete)
	 */
	@Transactional
	public void deleteNote(Long userId, Long noteId) {
		Note note = noteRepository.findById(noteId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));

		note.validateAuthor(userId);
		note.softDelete();
	}

	/**
	 * 내 글 검색
	 */
	@Transactional(readOnly = true)
	public Page<NoteSummaryResult> searchMyNotes(Long userId, NoteSearchQuery query) {
		validateSearchQuery(query);

		return noteRepository.searchMyNotes(userId, query.keyword(), query.pageable())
				.map(NoteSummaryResult::from);
	}


	/**
	 * 전체(공개) 검색
	 */
	@Transactional(readOnly = true)
	public Page<NoteSummaryResult> searchPublicNotes(NoteSearchQuery query) {
		validateSearchQuery(query);

		Page<Note> page = noteRepository.searchPublicNotes(query.keyword(), query.pageable());

		return page.map(NoteSummaryResult::from);
	}

	/**
	 * 폴더 변경
	 */
	@Transactional
	public NoteDetailResult moveNote(NoteMoveCommand command) {
		Note note = noteRepository.findById(command.noteId())
				.orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));

		Long sourceFolderId = note.getFolder() != null ? note.getFolder().getId() : null;
		Long targetFolderId = command.targetFolderId();

		Map<Long, Folder> lockedFolders = folderLockService.lockActiveFolders(
				Arrays.asList(sourceFolderId, targetFolderId)
		);

		Folder targetFolder = (targetFolderId == null) ? null : lockedFolders.get(targetFolderId);

		note.changeFolder(command.userId(), targetFolder);
		return NoteDetailResult.from(note);
	}

	/**
	 * 노트 이름 변경
	 */
	@Transactional
	public NoteDetailResult renameNote(NoteRenameCommand command) {
		Note note = noteRepository.findById(command.noteId())
				.orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));

		note.renameTitle(command.userId(), command.title());
		return NoteDetailResult.from(note);
	}

	private void ensureReadable(Note note, Long viewerId) {
		if (viewerId != null && note.isAuthor(viewerId)) {
			return;
		}

		if (note.getVisibility() == NoteVisibility.PUBLIC) {
			return;
		}

		throw new BusinessException(ErrorCode.NOTE_ACCESS_DENIED);
	}

	private static void validateSearchQuery(NoteSearchQuery query) {
		if (query == null || !query.hasKeyword()) {
			throw new BusinessException(ErrorCode.NOTE_SEARCH_KEYWORD_REQUIRED);
		}
	}
}
