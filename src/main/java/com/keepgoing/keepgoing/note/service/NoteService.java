package com.keepgoing.keepgoing.note.service;

import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.note.domain.Note;
import com.keepgoing.keepgoing.note.repository.NoteRepository;
import com.keepgoing.keepgoing.note.service.dto.*;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class NoteService {

	private final NoteRepository noteRepository;
	private final UserRepository userRepository;

	/**
	 * 노트 생성
	 */
	public NoteDetailResult createNote(NoteCreateCommand command) {
		User author = userRepository.findById(command.userId())
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		Note note = Note.create(
				author,
				command.title(),
				command.content(),
				command.visibility(),
				command.aiCollectable()
		);
		Note saved = noteRepository.save(note);
		return toDetailResult(saved);
	}

	/**
	 * 단일 노트 조회
	 */
	@Transactional(readOnly = true)
	public NoteDetailResult getNote(Long noteId) {
		Note note = noteRepository.findById(noteId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));
		return toDetailResult(note);
	}

	/**
	 * 노트 목록 조회
	 */
	@Transactional(readOnly = true)
	public Page<NoteSummaryResult> getNotes(Long userId, Pageable pageable) {
		return noteRepository.findByAuthor_Id(userId, pageable)
				.map(this::toSummaryResult);
	}

	/**
	 * 노트 수정
	 */
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

		return toDetailResult(note);
	}

	/**
	 * 노트 삭제 (soft delete)
	 */
	public void deleteNote(Long userId, Long noteId) {
		Note note = noteRepository.findById(noteId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));

		note.validateAuthor(userId);
		note.softDelete(); // deleted_at만 채움 → @Where 때문에 이후 조회에서 빠짐
	}

	/**
	 * 내 글 검색
	 */
	@Transactional(readOnly = true)
	public Page<NoteSummaryResult> searchMyNotes(Long userId, NoteSearchQuery query) {
		if (query == null || !query.hasKeyword()) {
			throw new BusinessException(ErrorCode.NOTE_SEARCH_KEYWORD_REQUIRED);
		}

		Page<Note> page = noteRepository.searchMyNotesFullText(
				userId,
				query.keyword(),
				query.pageable()
		);

		return page.map(this::toSummaryResult);
	}

	/**
	 * 전체(공개/공통) 검색
	 * <p>
	 * NOTE: MySQL에서 TEXT/MEDIUMTEXT 컬럼(content)이 CLOB로 매핑될 때, IgnoreCase 파생 쿼리는 upper()/lower()를 사용하며 오류가 날 수 있어
	 * Containing(대소문자 구분은 collation에 위임) 형태로 유지합니다.
	 */
	@Transactional(readOnly = true)
	public Page<NoteSummaryResult> searchNote(NoteSearchQuery query) {
		if (query == null || !query.hasKeyword()) {
			throw new BusinessException(ErrorCode.NOTE_SEARCH_KEYWORD_REQUIRED);
		}

		Page<Note> page = noteRepository.findByTitleContainingOrContentContaining(
				query.keyword(),
				query.keyword(),
				query.pageable()
		);

		return page.map(this::toSummaryResult);
	}

	private NoteDetailResult toDetailResult(Note note) {
		return new NoteDetailResult(
				note.getId(),
				note.getAuthor().getId(),
				note.getTitle(),
				note.getContent(),
				note.getVisibility(),
				note.isAiCollectable(),
				note.getCreatedAt(),
				note.getUpdatedAt()
		);
	}

	private NoteSummaryResult toSummaryResult(Note note) {
		return new NoteSummaryResult(
				note.getId(),
				note.getTitle(),
				note.getVisibility(),
				note.isAiCollectable(),
				note.getCreatedAt()
		);
	}
}
