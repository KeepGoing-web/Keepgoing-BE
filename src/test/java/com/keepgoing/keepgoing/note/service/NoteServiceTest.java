package com.keepgoing.keepgoing.note.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.keepgoing.keepgoing.folder.domain.Folder;
import com.keepgoing.keepgoing.folder.repository.FolderRepository;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.note.domain.Note;
import com.keepgoing.keepgoing.note.domain.NoteVisibility;
import com.keepgoing.keepgoing.note.repository.NoteRepository;
import com.keepgoing.keepgoing.note.service.dto.NoteCreateCommand;
import com.keepgoing.keepgoing.note.service.dto.NoteDetailResult;
import com.keepgoing.keepgoing.note.service.dto.NoteSearchQuery;
import com.keepgoing.keepgoing.note.service.dto.NoteSummaryResult;
import com.keepgoing.keepgoing.note.service.dto.NoteUpdateCommand;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class NoteServiceTest {

	public static final String DIRECTORY_NAME = "backend";
	@Mock
	NoteRepository noteRepository;

	@Mock
	UserRepository userRepository;

	@Mock
	FolderRepository folderRepository;

	@InjectMocks
	NoteService noteService;

	// ===== 테스트용 헬퍼 메서드들 =====

	private User createUser(Long id) {
		return User.builder()
				.id(id)
				.email("test@example.com")
				.name("테스트유저")
				.build();
	}

	// ========== createNote ==========

	@Test
	@DisplayName("createNote: folderId가 null이면 루트 노트를 생성한다.")
	void createNote_createsRootNoteWhenFolderIdIsNull() {
		// given
		Long userId = 1L;
		var command = NoteCreateCommand.builder()
				.userId(userId)
				.folderId(null)
				.title("제목")
				.content("내용")
				.visibility(NoteVisibility.PRIVATE)
				.aiCollectable(true)
				.build();
		User author = createUser(userId);

		given(userRepository.findById(userId)).willReturn(Optional.of(author));
		given(noteRepository.save(any(Note.class)))
				.willAnswer(invocation -> invocation.getArgument(0));

		// when
		NoteDetailResult result = noteService.createNote(command);

		// then
		ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
		verify(noteRepository).save(noteCaptor.capture());

		Note savedNote = noteCaptor.getValue();
		assertThat(savedNote.getAuthor().getId()).isEqualTo(userId);
		assertThat(savedNote.getFolder()).isNull();
		assertThat(savedNote.getTitle()).isEqualTo(command.title());
		assertThat(savedNote.getContent()).isEqualTo(command.content());
		assertThat(savedNote.getVisibility()).isEqualTo(command.visibility());
		assertThat(savedNote.isAiCollectable()).isEqualTo(command.aiCollectable());

		assertThat(result.userId()).isEqualTo(userId);
		assertThat(result.title()).isEqualTo(command.title());
		assertThat(result.folderId()).isNull();
		assertThat(result.content()).isEqualTo(command.content());
		assertThat(result.visibility()).isEqualTo(command.visibility());
		assertThat(result.aiCollectable()).isEqualTo(command.aiCollectable());

		verify(userRepository).findById(userId);
		verifyNoInteractions(folderRepository);
		verifyNoMoreInteractions(userRepository, noteRepository);
	}

	@Test
	@DisplayName("createNote: 유저가 없으면 USER_NOT_FOUND 예외 발생")
	void createNote_throwsWhenUserNotFound() {
		// given
		Long userId = 1L;
		String title = "제목";
		String content = "내용";
		NoteVisibility visibility = NoteVisibility.PRIVATE;
		boolean aiCollectable = false;

		given(userRepository.findById(userId)).willReturn(Optional.empty());

		NoteCreateCommand command = new NoteCreateCommand(
				userId,
				null,
				title,
				content,
				visibility,
				aiCollectable
		);
		// when & then
		assertThatThrownBy(() -> noteService.createNote(command))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
		verify(userRepository).findById(userId);
		verifyNoInteractions(noteRepository);
	}


	@Test
	@DisplayName("createNote: 내 폴더가 주어지면 해당 폴더에 노트를 생성한다.")
	void createNote_createsNoteWhenFolderExists() {
		// given
		Long userId = 1L;
		User author = createUser(1L);

		Folder folder = Folder.create(author, null, DIRECTORY_NAME);
		ReflectionTestUtils.setField(folder, "id", 2L);

		var command = NoteCreateCommand.builder()
				.userId(userId)
				.folderId(folder.getId())
				.title("제목")
				.content("내용")
				.visibility(NoteVisibility.PRIVATE)
				.aiCollectable(false)
				.build();

		given(userRepository.findById(command.userId()))
				.willReturn(Optional.of(author));
		given(folderRepository.findByIdAndDeletedAtIsNull(command.folderId()))
				.willReturn(Optional.of(folder));
		given(noteRepository.save(any(Note.class)))
				.willAnswer(invocation -> invocation.getArgument(0));

		// when
		NoteDetailResult result = noteService.createNote(command);

		// then
		ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
		verify(noteRepository).save(noteCaptor.capture());

		Note savedNote = noteCaptor.getValue();
		assertThat(savedNote.getAuthor().getId()).isEqualTo(userId);
		assertThat(savedNote.getFolder()).isNotNull();
		assertThat(savedNote.getFolder().getId()).isEqualTo(folder.getId());
		assertThat(savedNote.getTitle()).isEqualTo(command.title());
		assertThat(savedNote.getContent()).isEqualTo(command.content());
		assertThat(savedNote.getVisibility()).isEqualTo(command.visibility());
		assertThat(savedNote.isAiCollectable()).isEqualTo(command.aiCollectable());

		assertThat(result.userId()).isEqualTo(userId);
		assertThat(result.title()).isEqualTo(command.title());
		assertThat(result.folderId()).isEqualTo(command.folderId());
		assertThat(result.content()).isEqualTo(command.content());
		assertThat(result.visibility()).isEqualTo(command.visibility());
		assertThat(result.aiCollectable()).isEqualTo(command.aiCollectable());
		verify(userRepository).findById(userId);
		verify(folderRepository).findByIdAndDeletedAtIsNull(folder.getId());
		verify(noteRepository).save(any(Note.class));
		verifyNoMoreInteractions(userRepository, noteRepository, folderRepository);
	}

	@Test
	@DisplayName("createNote: 존재하지 않는 폴더면 FOLDER_NOT_FOUND 예외가 발생한다")
	void createNote_throwsWhenFolderNotFound() {
		// given
		Long userId = 1L;
		User author = createUser(1L);
		var command = NoteCreateCommand.builder()
				.userId(userId)
				.folderId(2L)
				.title("제목")
				.content("내용")
				.visibility(NoteVisibility.PRIVATE)
				.aiCollectable(false)
				.build();
		given(userRepository.findById(command.userId()))
				.willReturn(Optional.of(author));
		given(folderRepository.findByIdAndDeletedAtIsNull(2L))
				.willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> noteService.createNote(command))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_NOT_FOUND);
		verify(userRepository).findById(command.userId());
		verify(folderRepository).findByIdAndDeletedAtIsNull(2L);
		verifyNoMoreInteractions(userRepository, folderRepository);
		verifyNoInteractions(noteRepository);
	}

	@Test
	@DisplayName("createNote: 다른 사용자의 폴더면 FOLDER_ACCESS_DENIED 예외가 발생한다")
	void createNote_throwsWhenFolderOwnedByAnotherUser() {
		// given
		Long userId = 1L;
		Long otherUserId = 2L;
		Long folderId = 10L;
		User author = createUser(1L);
		User otherAuthor = createUser(otherUserId);
		Folder folder = Folder.create(
				otherAuthor,
				null,
				DIRECTORY_NAME
		);
		ReflectionTestUtils.setField(folder, "id", folderId);
		var command = NoteCreateCommand.builder()
				.userId(userId)
				.folderId(folderId)
				.title("제목")
				.content("내용")
				.visibility(NoteVisibility.PRIVATE)
				.aiCollectable(false)
				.build();
		given(userRepository.findById(command.userId()))
				.willReturn(Optional.of(author));
		given(folderRepository.findByIdAndDeletedAtIsNull(folderId))
				.willReturn(Optional.of(folder));

		// when & then
		assertThatThrownBy(() -> noteService.createNote(command))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_ACCESS_DENIED);
		verify(userRepository).findById(command.userId());
		verify(folderRepository).findByIdAndDeletedAtIsNull(folderId);
		verifyNoMoreInteractions(userRepository, folderRepository);
		verifyNoInteractions(noteRepository);
	}

	// ========== getNote ==========

	@Test
	@DisplayName("getNote: 게시글이 존재하면 반환한다")
	void getNote_returnsNoteWhenExists() {
		// given
		Long noteId = 1L;
		User user = createUser(1L);
		Note note = Note.create(user, null, "제목", "내용", NoteVisibility.PRIVATE, true);

		given(noteRepository.findById(noteId)).willReturn(Optional.of(note));

		// when
		NoteDetailResult result = noteService.getNote(noteId);

		// then
		assertThat(result.title()).isEqualTo("제목");
		assertThat(result.content()).isEqualTo("내용");
		assertThat(result.visibility()).isEqualTo(NoteVisibility.PRIVATE);

		verify(noteRepository).findById(noteId);
		verifyNoMoreInteractions(noteRepository);
		verifyNoInteractions(userRepository);
	}

	@Test
	@DisplayName("getNote: 게시글이 없으면 NOTE_NOT_FOUND 예외 발생")
	void getNote_throwsWhenNotFound() {
		// given
		Long noteId = 1L;
		given(noteRepository.findById(noteId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> noteService.getNote(noteId))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_NOT_FOUND);
		verify(noteRepository).findById(noteId);
		verifyNoMoreInteractions(noteRepository);
		verifyNoInteractions(userRepository);

	}

	// ========== getNotes ==========

	@Test
	@DisplayName("getNotes: 작성자 ID와 Pageable로 게시글 페이지를 가져온다")
	void getNotes_returnsPage() {
		// given
		Long userId = 1L;
		User user = createUser(userId);

		Note note1 = Note.create(user, null, "제목1", "내용1", NoteVisibility.PRIVATE, true);
		Note note2 = Note.create(user, null, "제목2", "내용2", NoteVisibility.PUBLIC, true);
		var notes = java.util.List.of(note1, note2);

		Pageable pageable = PageRequest.of(
				0,
				10,
				Sort.by(Sort.Direction.DESC, "createdAt")
		);
		Page<Note> notePage = new PageImpl<>(
				notes,
				pageable,
				notes.size()
		);

		// 저장소가 페이지를 반환하는 동작을 스텁
		given(noteRepository.findByAuthor_Id(userId, pageable))
				.willReturn(notePage);

		// when
		Page<NoteSummaryResult> result = noteService.getNotes(userId, pageable);

		// then
		assertThat(result.getTotalElements()).isEqualTo(2);
		assertThat(result.getContent()).hasSize(2);
		assertThat(result.getContent()
				.get(0)
				.title()).isEqualTo("제목1");
		assertThat(result.getContent()
				.get(1)
				.title()).isEqualTo("제목2");
		verify(noteRepository).findByAuthor_Id(userId, pageable);
		verifyNoMoreInteractions(noteRepository);
		verifyNoInteractions(userRepository);
	}

	// ========== updateNote ==========

	@Test
	@DisplayName("updateNote: 작성자가 맞으면 게시글이 수정된다")
	void updateNote_updatesWhenAuthorMatches() {
		// given
		Long userId = 1L;
		Long noteId = 10L;

		User user = createUser(userId);
		Note note = Note.create(user, null, "old", "old", NoteVisibility.PRIVATE, true);

		String newTitle = "수정 제목";
		String newContent = "수정 내용";
		NoteVisibility newVisibility = NoteVisibility.PUBLIC;
		boolean newAiCollectable = false;

		given(noteRepository.findById(noteId)).willReturn(Optional.of(note));

		NoteUpdateCommand command = new NoteUpdateCommand(
				noteId,
				userId,
				newTitle,
				newContent,
				newVisibility,
				newAiCollectable
		);

		// when
		NoteDetailResult result = noteService.updateNote(command);

		// then
		assertThat(result.title()).isEqualTo(newTitle);
		assertThat(result.content()).isEqualTo(newContent);
		assertThat(result.visibility()).isEqualTo(newVisibility);
		assertThat(result.aiCollectable()).isEqualTo(newAiCollectable);
		verify(noteRepository).findById(noteId);
		verifyNoMoreInteractions(noteRepository);
		verifyNoInteractions(userRepository);
	}

	@Test
	@DisplayName("updateNote: 게시글이 없으면 NOTE_NOT_FOUND 예외")
	void updateNote_throwsWhenNoteNotFound() {
		// given
		Long userId = 1L;
		Long noteId = 10L;

		String newTitle = "수정 제목";
		String newContent = "수정 내용";
		NoteVisibility newVisibility = NoteVisibility.PUBLIC;
		boolean newAiCollectable = false;

		given(noteRepository.findById(noteId)).willReturn(Optional.empty());

		NoteUpdateCommand command = new NoteUpdateCommand(
				noteId,
				userId,
				newTitle,
				newContent,
				newVisibility,
				newAiCollectable
		);

		// when & then
		assertThatThrownBy(() -> noteService.updateNote(command))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_NOT_FOUND);
		verify(noteRepository).findById(noteId);
		verifyNoMoreInteractions(noteRepository);
		verifyNoInteractions(userRepository);
	}

	@Test
	@DisplayName("updateNote: 작성자가 아니면 NOTE_ACCESS_DENIED 예외")
	void updateNote_throwsWhenNotAuthor() {
		// given
		Long userId = 1L;
		Long othersId = 2L;
		Long noteId = 10L;

		User user = createUser(othersId); // 실제 작성자는 2번
		Note note = Note.create(user, null, "old", "old", NoteVisibility.PRIVATE, true);

		String newTitle = "수정 제목";
		String newContent = "수정 내용";
		NoteVisibility newVisibility = NoteVisibility.PUBLIC;
		boolean newAiCollectable = false;

		given(noteRepository.findById(noteId)).willReturn(Optional.of(note));

		NoteUpdateCommand command = new NoteUpdateCommand(
				noteId,
				userId,
				newTitle,
				newContent,
				newVisibility,
				newAiCollectable
		);

		// when & then
		assertThatThrownBy(() -> noteService.updateNote(command))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_ACCESS_DENIED);
		verify(noteRepository).findById(noteId);
		verifyNoMoreInteractions(noteRepository);
		verifyNoInteractions(userRepository);
	}

	// ========== deleteNote ==========

	@Test
	@DisplayName("deleteNote: 작성자가 맞으면 softDelete 된다")
	void deleteNote_softDeletesWhenAuthorMatches() {
		// given
		Long userId = 1L;
		Long noteId = 10L;

		User user = createUser(userId);
		Note note = Note.create(user, null, "title", "content", NoteVisibility.PRIVATE, true);

		given(noteRepository.findById(noteId)).willReturn(Optional.of(note));

		// when
		noteService.deleteNote(userId, noteId);

		// then
		assertThat(note.isDeleted()).isTrue(); // isDeleted 없으면 deletedAt != null 로 체크
		verify(noteRepository).findById(noteId);
		verifyNoMoreInteractions(noteRepository);
		verifyNoInteractions(userRepository);
	}

	@Test
	@DisplayName("deleteNote: 게시글이 없으면 NOTE_NOT_FOUND 예외")
	void deleteNote_throwsWhenNoteNotFound() {
		// given
		Long userId = 1L;
		Long noteId = 10L;

		given(noteRepository.findById(noteId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> noteService.deleteNote(userId, noteId))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_NOT_FOUND);
		verify(noteRepository).findById(noteId);
		verifyNoMoreInteractions(noteRepository);
		verifyNoInteractions(userRepository);
	}

	@Test
	@DisplayName("deleteNote: 작성자가 아니면 NOTE_ACCESS_DENIED 예외")
	void deleteNote_throwsWhenNotAuthor() {
		// given
		Long userId = 1L;
		Long othersId = 2L;
		Long noteId = 10L;

		User user = createUser(othersId); // 작성자는 2번
		Note note = Note.create(user, null, "title", "content", NoteVisibility.PRIVATE, true);

		given(noteRepository.findById(noteId)).willReturn(Optional.of(note));

		// when & then
		assertThatThrownBy(() -> noteService.deleteNote(userId, noteId))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_ACCESS_DENIED);
		verify(noteRepository).findById(noteId);
		verifyNoMoreInteractions(noteRepository);
		verifyNoInteractions(userRepository);
	}

	// ========== searchMyNotes ==========

	@Test
	@DisplayName("searchMyNotes: keyword가 있으면 검색 결과를 페이지로 반환한다")
	void searchMyNotes_returnsPageWhenKeywordProvided() {
		// given
		Long userId = 1L;
		User user = createUser(userId);

		Note note1 = Note.create(user, null, "spring 제목", "내용", NoteVisibility.PUBLIC, true);
		Note note2 = Note.create(user, null, "제목", "spring 내용", NoteVisibility.PUBLIC, true);

		Pageable pageable = PageRequest.of(0, 10);
		Page<Note> notePage = new PageImpl<>(List.of(note1, note2), pageable, 2);

		NoteSearchQuery query = new NoteSearchQuery("spring", pageable);

		given(noteRepository.searchMyNotes(eq(userId), eq("spring"), any(Pageable.class)))
				.willReturn(notePage);

		// when
		Page<NoteSummaryResult> result = noteService.searchMyNotes(userId, query);

		// then
		assertThat(result.getTotalElements()).isEqualTo(2);
		assertThat(result.getContent()).hasSize(2);
		verify(noteRepository).searchMyNotes(eq(userId), eq("spring"), any(Pageable.class));
		verifyNoMoreInteractions(noteRepository);
		verifyNoInteractions(userRepository);
	}

	@Test
	@DisplayName("searchMyNotes: keyword가 비어있으면 NOTE_SEARCH_KEYWORD_REQUIRED 예외")
	void searchMyNotes_throwsWhenKeywordBlank() {
		// given
		Long userId = 1L;
		Pageable pageable = PageRequest.of(0, 10);
		NoteSearchQuery query = new NoteSearchQuery("   ", pageable);

		// when & then
		assertThatThrownBy(() -> noteService.searchMyNotes(userId, query))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_SEARCH_KEYWORD_REQUIRED);

		verifyNoInteractions(noteRepository);
		verifyNoInteractions(userRepository);
	}

	@Test
	@DisplayName("searchMyNotes: keyword 앞뒤 공백은 trim되어 검색된다")
	void searchMyNotes_trimsKeywordBeforeSearching() {
		// given
		Long userId = 1L;
		User user = createUser(userId);
		Note note = Note.create(user, null, "spring 제목", "내용", NoteVisibility.PUBLIC, true);

		Pageable pageable = PageRequest.of(0, 10);
		Page<Note> notePage = new PageImpl<>(List.of(note), pageable, 1);

		NoteSearchQuery query = new NoteSearchQuery("  spring  ", pageable);

		given(noteRepository.searchMyNotes(eq(userId), eq("spring"), any(Pageable.class)))
				.willReturn(notePage);

		// when
		Page<NoteSummaryResult> result = noteService.searchMyNotes(userId, query);

		// then
		assertThat(result.getTotalElements()).isEqualTo(1);

		ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
		verify(noteRepository).searchMyNotes(eq(userId), keywordCaptor.capture(), any(Pageable.class));
		assertThat(keywordCaptor.getValue()).isEqualTo("spring");

		verifyNoMoreInteractions(noteRepository);
		verifyNoInteractions(userRepository);
	}
}
