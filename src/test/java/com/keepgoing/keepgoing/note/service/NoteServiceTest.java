package com.keepgoing.keepgoing.note.service;

import static com.keepgoing.keepgoing.support.UserFixture.user;
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
import com.keepgoing.keepgoing.note.service.dto.NoteMoveCommand;
import com.keepgoing.keepgoing.note.service.dto.NoteSearchQuery;
import com.keepgoing.keepgoing.note.service.dto.NoteSummaryResult;
import com.keepgoing.keepgoing.note.service.dto.NoteUpdateCommand;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
class NoteServiceTest {

	private static final String DIRECTORY_NAME = "backend";

	@Mock
	NoteRepository noteRepository;

	@Mock
	UserRepository userRepository;

	@Mock
	FolderRepository folderRepository;

	@InjectMocks
	NoteService noteService;

	@Nested
	@DisplayName("노트 생성")
	class CreateNote {

		@Test
		@DisplayName("folderId가 null이면 루트 노트를 생성한다")
		void createsRootNoteWhenFolderIdIsNull() {
			Long userId = 1L;
			NoteCreateCommand command = NoteCreateCommand.builder()
					.userId(userId)
					.folderId(null)
					.title("제목")
					.content("내용")
					.visibility(NoteVisibility.PRIVATE)
					.aiCollectable(true)
					.build();
			User author = user(userId, "test@example.com", "테스트유저");

			given(userRepository.findById(userId)).willReturn(Optional.of(author));
			given(noteRepository.save(any(Note.class)))
					.willAnswer(invocation -> invocation.getArgument(0));

			NoteDetailResult result = noteService.createNote(command);

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
		@DisplayName("유저가 없으면 USER_NOT_FOUND 예외가 발생한다")
		void throwsWhenUserNotFound() {
			Long userId = 1L;
			NoteCreateCommand command = new NoteCreateCommand(
					userId,
					null,
					"제목",
					"내용",
					NoteVisibility.PRIVATE,
					false
			);

			given(userRepository.findById(userId)).willReturn(Optional.empty());

			assertThatThrownBy(() -> noteService.createNote(command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
			verify(userRepository).findById(userId);
			verifyNoInteractions(noteRepository, folderRepository);
		}

		@Test
		@DisplayName("내 폴더가 주어지면 해당 폴더에 노트를 생성한다")
		void createsNoteWhenFolderExists() {
			Long userId = 1L;
			User author = user(userId, "test@example.com", "테스트유저");
			Folder folder = folder(author, 2L);
			NoteCreateCommand command = NoteCreateCommand.builder()
					.userId(userId)
					.folderId(folder.getId())
					.title("제목")
					.content("내용")
					.visibility(NoteVisibility.PRIVATE)
					.aiCollectable(false)
					.build();

			given(userRepository.findById(command.userId())).willReturn(Optional.of(author));
			given(folderRepository.findByIdAndDeletedAtIsNull(command.folderId()))
					.willReturn(Optional.of(folder));
			given(noteRepository.save(any(Note.class)))
					.willAnswer(invocation -> invocation.getArgument(0));

			NoteDetailResult result = noteService.createNote(command);

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
		@DisplayName("존재하지 않는 폴더면 FOLDER_NOT_FOUND 예외가 발생한다")
		void throwsWhenFolderNotFound() {
			Long userId = 1L;
			User author = user(userId);
			NoteCreateCommand command = NoteCreateCommand.builder()
					.userId(userId)
					.folderId(2L)
					.title("제목")
					.content("내용")
					.visibility(NoteVisibility.PRIVATE)
					.aiCollectable(false)
					.build();

			given(userRepository.findById(command.userId())).willReturn(Optional.of(author));
			given(folderRepository.findByIdAndDeletedAtIsNull(2L)).willReturn(Optional.empty());

			assertThatThrownBy(() -> noteService.createNote(command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_NOT_FOUND);
			verify(userRepository).findById(command.userId());
			verify(folderRepository).findByIdAndDeletedAtIsNull(2L);
			verifyNoMoreInteractions(userRepository, folderRepository);
			verifyNoInteractions(noteRepository);
		}

		@Test
		@DisplayName("다른 사용자의 폴더면 FOLDER_ACCESS_DENIED 예외가 발생한다")
		void throwsWhenFolderOwnedByAnotherUser() {
			Long userId = 1L;
			Long otherUserId = 2L;
			Long folderId = 10L;
			User author = user(userId);
			User otherAuthor = user(otherUserId);
			Folder folder = folder(otherAuthor, folderId);
			NoteCreateCommand command = NoteCreateCommand.builder()
					.userId(userId)
					.folderId(folderId)
					.title("제목")
					.content("내용")
					.visibility(NoteVisibility.PRIVATE)
					.aiCollectable(false)
					.build();

			given(userRepository.findById(command.userId())).willReturn(Optional.of(author));
			given(folderRepository.findByIdAndDeletedAtIsNull(folderId)).willReturn(Optional.of(folder));

			assertThatThrownBy(() -> noteService.createNote(command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_ACCESS_DENIED);
			verify(userRepository).findById(command.userId());
			verify(folderRepository).findByIdAndDeletedAtIsNull(folderId);
			verifyNoMoreInteractions(userRepository, folderRepository);
			verifyNoInteractions(noteRepository);
		}
	}

	@Nested
	@DisplayName("단건 조회")
	class GetNote {

		@Test
		@DisplayName("작성자 본인은 비공개 노트를 조회할 수 있다")
		void returnsPrivateNoteForAuthor() {
			Long noteId = 1L;
			Long viewerId = 1L;
			User author = user(viewerId);
			Note note = Note.create(author, null, "제목", "내용", NoteVisibility.PRIVATE, true);

			given(noteRepository.findById(noteId)).willReturn(Optional.of(note));

			NoteDetailResult result = noteService.getNote(viewerId, noteId);

			assertThat(result.title()).isEqualTo("제목");
			assertThat(result.content()).isEqualTo("내용");
			assertThat(result.visibility()).isEqualTo(NoteVisibility.PRIVATE);
			verify(noteRepository).findById(noteId);
			verifyNoMoreInteractions(noteRepository);
			verifyNoInteractions(userRepository, folderRepository);
		}

		@Test
		@DisplayName("익명 사용자는 공개 노트를 조회할 수 있다")
		void returnsPublicNoteForAnonymousViewer() {
			Long noteId = 1L;
			User author = user(2L);
			Note note = Note.create(author, null, "제목", "내용", NoteVisibility.PUBLIC, true);

			given(noteRepository.findById(noteId)).willReturn(Optional.of(note));

			NoteDetailResult result = noteService.getNote(null, noteId);

			assertThat(result.title()).isEqualTo("제목");
			assertThat(result.visibility()).isEqualTo(NoteVisibility.PUBLIC);
			verify(noteRepository).findById(noteId);
			verifyNoMoreInteractions(noteRepository);
			verifyNoInteractions(userRepository, folderRepository);
		}

		@Test
		@DisplayName("익명 사용자가 비공개 노트를 조회하면 NOTE_ACCESS_DENIED 예외가 발생한다")
		void throwsWhenAnonymousViewerRequestsPrivateNote() {
			Long noteId = 1L;
			User author = user(2L);
			Note note = Note.create(author, null, "제목", "내용", NoteVisibility.PRIVATE, true);

			given(noteRepository.findById(noteId)).willReturn(Optional.of(note));

			assertThatThrownBy(() -> noteService.getNote(null, noteId))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_ACCESS_DENIED);
			verify(noteRepository).findById(noteId);
			verifyNoMoreInteractions(noteRepository);
			verifyNoInteractions(userRepository, folderRepository);
		}

		@Test
		@DisplayName("다른 사용자는 공개 노트를 조회할 수 있다")
		void returnsPublicNoteForDifferentViewer() {
			Long noteId = 1L;
			Long viewerId = 1L;
			User author = user(2L);
			Note note = Note.create(author, null, "제목", "내용", NoteVisibility.PUBLIC, true);

			given(noteRepository.findById(noteId)).willReturn(Optional.of(note));

			NoteDetailResult result = noteService.getNote(viewerId, noteId);

			assertThat(result.title()).isEqualTo("제목");
			assertThat(result.visibility()).isEqualTo(NoteVisibility.PUBLIC);
			verify(noteRepository).findById(noteId);
			verifyNoMoreInteractions(noteRepository);
			verifyNoInteractions(userRepository, folderRepository);
		}

		@Test
		@DisplayName("다른 사용자가 비공개 노트를 조회하면 NOTE_ACCESS_DENIED 예외가 발생한다")
		void throwsWhenDifferentViewerRequestsPrivateNote() {
			Long noteId = 1L;
			Long viewerId = 1L;
			User author = user(2L);
			Note note = Note.create(author, null, "제목", "내용", NoteVisibility.PRIVATE, true);

			given(noteRepository.findById(noteId)).willReturn(Optional.of(note));

			assertThatThrownBy(() -> noteService.getNote(viewerId, noteId))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_ACCESS_DENIED);
			verify(noteRepository).findById(noteId);
			verifyNoMoreInteractions(noteRepository);
			verifyNoInteractions(userRepository, folderRepository);
		}

		@Test
		@DisplayName("게시글이 없으면 NOTE_NOT_FOUND 예외가 발생한다")
		void throwsWhenNotFound() {
			Long viewerId = 1L;
			Long noteId = 1L;
			given(noteRepository.findById(noteId)).willReturn(Optional.empty());

			assertThatThrownBy(() -> noteService.getNote(viewerId, noteId))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_NOT_FOUND);
			verify(noteRepository).findById(noteId);
			verifyNoMoreInteractions(noteRepository);
			verifyNoInteractions(userRepository, folderRepository);
		}
	}

	@Nested
	@DisplayName("목록 조회")
	class GetNotes {

		@Test
		@DisplayName("작성자 ID와 Pageable로 게시글 페이지를 가져온다")
		void returnsPage() {
			Long userId = 1L;
			User user = user(userId);
			Folder folder = folder(user, 10L);
			Note note1 = Note.create(user, folder, "제목1", "내용1", NoteVisibility.PRIVATE, true);
			Note note2 = Note.create(user, null, "제목2", "내용2", NoteVisibility.PUBLIC, true);
			List<Note> notes = List.of(note1, note2);
			Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
			Page<Note> notePage = new PageImpl<>(notes, pageable, notes.size());

			given(noteRepository.findByAuthor_Id(userId, pageable)).willReturn(notePage);

			Page<NoteSummaryResult> result = noteService.getNotes(userId, pageable);

			assertThat(result.getTotalElements()).isEqualTo(2);
			assertThat(result.getContent()).hasSize(2);
			assertThat(result.getContent().get(0).title()).isEqualTo("제목1");
			assertThat(result.getContent().get(0).folderId()).isEqualTo(10L);
			assertThat(result.getContent().get(1).title()).isEqualTo("제목2");
			assertThat(result.getContent().get(1).folderId()).isNull();
			verify(noteRepository).findByAuthor_Id(userId, pageable);
			verifyNoMoreInteractions(noteRepository);
			verifyNoInteractions(userRepository, folderRepository);
		}
	}

	@Nested
	@DisplayName("노트 수정")
	class UpdateNote {

		@Test
		@DisplayName("작성자가 맞으면 게시글이 수정된다")
		void updatesWhenAuthorMatches() {
			Long userId = 1L;
			Long noteId = 10L;
			User user = user(userId);
			Note note = Note.create(user, null, "old", "old", NoteVisibility.PRIVATE, true);
			String newTitle = "수정 제목";
			String newContent = "수정 내용";
			NoteVisibility newVisibility = NoteVisibility.PUBLIC;
			boolean newAiCollectable = false;
			NoteUpdateCommand command = new NoteUpdateCommand(
					noteId,
					userId,
					newTitle,
					newContent,
					newVisibility,
					newAiCollectable
			);

			given(noteRepository.findById(noteId)).willReturn(Optional.of(note));

			NoteDetailResult result = noteService.updateNote(command);

			assertThat(result.title()).isEqualTo(newTitle);
			assertThat(result.content()).isEqualTo(newContent);
			assertThat(result.visibility()).isEqualTo(newVisibility);
			assertThat(result.aiCollectable()).isEqualTo(newAiCollectable);
			verify(noteRepository).findById(noteId);
			verifyNoMoreInteractions(noteRepository);
			verifyNoInteractions(userRepository, folderRepository);
		}

		@Test
		@DisplayName("게시글이 없으면 NOTE_NOT_FOUND 예외가 발생한다")
		void throwsWhenNoteNotFound() {
			Long userId = 1L;
			Long noteId = 10L;
			NoteUpdateCommand command = new NoteUpdateCommand(
					noteId,
					userId,
					"수정 제목",
					"수정 내용",
					NoteVisibility.PUBLIC,
					false
			);

			given(noteRepository.findById(noteId)).willReturn(Optional.empty());

			assertThatThrownBy(() -> noteService.updateNote(command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_NOT_FOUND);
			verify(noteRepository).findById(noteId);
			verifyNoMoreInteractions(noteRepository);
			verifyNoInteractions(userRepository, folderRepository);
		}

		@Test
		@DisplayName("작성자가 아니면 NOTE_ACCESS_DENIED 예외가 발생한다")
		void throwsWhenNotAuthor() {
			Long requesterId = 1L;
			Long authorId = 2L;
			Long noteId = 10L;
			User author = user(authorId);
			Note note = Note.create(author, null, "old", "old", NoteVisibility.PRIVATE, true);
			NoteUpdateCommand command = new NoteUpdateCommand(
					noteId,
					requesterId,
					"수정 제목",
					"수정 내용",
					NoteVisibility.PUBLIC,
					false
			);

			given(noteRepository.findById(noteId)).willReturn(Optional.of(note));

			assertThatThrownBy(() -> noteService.updateNote(command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_ACCESS_DENIED);
			verify(noteRepository).findById(noteId);
			verifyNoMoreInteractions(noteRepository);
			verifyNoInteractions(userRepository, folderRepository);
		}
	}

	@Nested
	@DisplayName("노트 삭제")
	class DeleteNote {

		@Test
		@DisplayName("작성자가 맞으면 soft delete 된다")
		void softDeletesWhenAuthorMatches() {
			Long userId = 1L;
			Long noteId = 10L;
			User user = user(userId);
			Note note = Note.create(user, null, "title", "content", NoteVisibility.PRIVATE, true);

			given(noteRepository.findById(noteId)).willReturn(Optional.of(note));

			noteService.deleteNote(userId, noteId);

			assertThat(note.isDeleted()).isTrue();
			verify(noteRepository).findById(noteId);
			verifyNoMoreInteractions(noteRepository);
			verifyNoInteractions(userRepository, folderRepository);
		}

		@Test
		@DisplayName("게시글이 없으면 NOTE_NOT_FOUND 예외가 발생한다")
		void throwsWhenNoteNotFound() {
			Long userId = 1L;
			Long noteId = 10L;

			given(noteRepository.findById(noteId)).willReturn(Optional.empty());

			assertThatThrownBy(() -> noteService.deleteNote(userId, noteId))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_NOT_FOUND);
			verify(noteRepository).findById(noteId);
			verifyNoMoreInteractions(noteRepository);
			verifyNoInteractions(userRepository, folderRepository);
		}

		@Test
		@DisplayName("작성자가 아니면 NOTE_ACCESS_DENIED 예외가 발생한다")
		void throwsWhenNotAuthor() {
			Long requesterId = 1L;
			Long authorId = 2L;
			Long noteId = 10L;
			User author = user(authorId);
			Note note = Note.create(author, null, "title", "content", NoteVisibility.PRIVATE, true);

			given(noteRepository.findById(noteId)).willReturn(Optional.of(note));

			assertThatThrownBy(() -> noteService.deleteNote(requesterId, noteId))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_ACCESS_DENIED);
			verify(noteRepository).findById(noteId);
			verifyNoMoreInteractions(noteRepository);
			verifyNoInteractions(userRepository, folderRepository);
		}
	}

	@Nested
	@DisplayName("노트 이동")
	class MoveNote {

		@Test
		@DisplayName("내 폴더로 이동하면 folderId가 변경된다")
		void movesToOwnedFolder() {
			Long userId = 1L;
			Long noteId = 10L;
			Long folderId = 20L;
			User author = user(userId);
			Note note = Note.create(author, null, "title", "content", NoteVisibility.PRIVATE, true);
			Folder folder = folder(author, folderId);
			NoteMoveCommand command = new NoteMoveCommand(noteId, userId, folderId);

			given(noteRepository.findById(noteId)).willReturn(Optional.of(note));
			given(folderRepository.findByIdAndDeletedAtIsNull(folderId)).willReturn(Optional.of(folder));

			NoteDetailResult result = noteService.moveNote(command);

			assertThat(note.getFolder()).isEqualTo(folder);
			assertThat(result.folderId()).isEqualTo(folderId);
			verify(noteRepository).findById(noteId);
			verify(folderRepository).findByIdAndDeletedAtIsNull(folderId);
			verifyNoMoreInteractions(noteRepository, folderRepository);
			verifyNoInteractions(userRepository);
		}

		@Test
		@DisplayName("folderId가 null이면 루트로 이동한다")
		void movesToRootWhenFolderIdIsNull() {
			Long userId = 1L;
			Long noteId = 10L;
			Long existingFolderId = 30L;
			User author = user(userId);
			Folder existingFolder = folder(author, existingFolderId);
			Note note = Note.create(author, existingFolder, "title", "content", NoteVisibility.PRIVATE, true);
			NoteMoveCommand command = new NoteMoveCommand(noteId, userId, null);

			given(noteRepository.findById(noteId)).willReturn(Optional.of(note));

			NoteDetailResult result = noteService.moveNote(command);

			assertThat(note.getFolder()).isNull();
			assertThat(result.folderId()).isNull();
			verify(noteRepository).findById(noteId);
			verifyNoMoreInteractions(noteRepository);
			verifyNoInteractions(folderRepository, userRepository);
		}

		@Test
		@DisplayName("노트가 없으면 NOTE_NOT_FOUND 예외가 발생한다")
		void throwsWhenNoteNotFound() {
			Long noteId = 10L;
			Long userId = 1L;
			Long folderId = 20L;
			NoteMoveCommand command = new NoteMoveCommand(noteId, userId, folderId);

			given(noteRepository.findById(noteId)).willReturn(Optional.empty());

			assertThatThrownBy(() -> noteService.moveNote(command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_NOT_FOUND);
			verify(noteRepository).findById(noteId);
			verifyNoMoreInteractions(noteRepository);
			verifyNoInteractions(folderRepository, userRepository);
		}

		@Test
		@DisplayName("대상 폴더가 없으면 FOLDER_NOT_FOUND 예외가 발생한다")
		void throwsWhenFolderNotFound() {
			Long userId = 1L;
			Long noteId = 10L;
			Long folderId = 20L;
			User author = user(userId);
			Note note = Note.create(author, null, "title", "content", NoteVisibility.PRIVATE, true);
			NoteMoveCommand command = new NoteMoveCommand(noteId, userId, folderId);

			given(noteRepository.findById(noteId)).willReturn(Optional.of(note));
			given(folderRepository.findByIdAndDeletedAtIsNull(folderId)).willReturn(Optional.empty());

			assertThatThrownBy(() -> noteService.moveNote(command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_NOT_FOUND);
			verify(noteRepository).findById(noteId);
			verify(folderRepository).findByIdAndDeletedAtIsNull(folderId);
			verifyNoMoreInteractions(noteRepository, folderRepository);
			verifyNoInteractions(userRepository);
		}

		@Test
		@DisplayName("작성자가 아니면 NOTE_ACCESS_DENIED 예외가 발생한다")
		void throwsWhenRequesterIsNotAuthor() {
			Long requesterId = 1L;
			Long authorId = 2L;
			Long noteId = 10L;
			User author = user(authorId);
			Note note = Note.create(author, null, "title", "content", NoteVisibility.PRIVATE, true);
			NoteMoveCommand command = new NoteMoveCommand(noteId, requesterId, null);

			given(noteRepository.findById(noteId)).willReturn(Optional.of(note));

			assertThatThrownBy(() -> noteService.moveNote(command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_ACCESS_DENIED);
			verify(noteRepository).findById(noteId);
			verifyNoMoreInteractions(noteRepository);
			verifyNoInteractions(folderRepository, userRepository);
		}

		@Test
		@DisplayName("다른 사용자의 폴더면 FOLDER_ACCESS_DENIED 예외가 발생한다")
		void throwsWhenTargetFolderOwnedByAnotherUser() {
			Long userId = 1L;
			Long otherUserId = 2L;
			Long noteId = 10L;
			Long folderId = 20L;
			User author = user(userId);
			User otherAuthor = user(otherUserId);
			Note note = Note.create(author, null, "title", "content", NoteVisibility.PRIVATE, true);
			Folder otherFolder = folder(otherAuthor, folderId);
			NoteMoveCommand command = new NoteMoveCommand(noteId, userId, folderId);

			given(noteRepository.findById(noteId)).willReturn(Optional.of(note));
			given(folderRepository.findByIdAndDeletedAtIsNull(folderId)).willReturn(Optional.of(otherFolder));

			assertThatThrownBy(() -> noteService.moveNote(command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_ACCESS_DENIED);
			verify(noteRepository).findById(noteId);
			verify(folderRepository).findByIdAndDeletedAtIsNull(folderId);
			verifyNoMoreInteractions(noteRepository, folderRepository);
			verifyNoInteractions(userRepository);
		}
	}

	@Nested
	@DisplayName("내 노트 검색")
	class SearchMyNotes {

		@Test
		@DisplayName("keyword가 있으면 검색 결과를 페이지로 반환한다")
		void returnsPageWhenKeywordProvided() {
			Long userId = 1L;
			User user = user(userId);
			Folder folder = folder(user, 10L);
			Note note1 = Note.create(user, folder, "spring 제목", "내용", NoteVisibility.PUBLIC, true);
			Note note2 = Note.create(user, null, "제목", "spring 내용", NoteVisibility.PUBLIC, true);
			Pageable pageable = PageRequest.of(0, 10);
			Page<Note> notePage = new PageImpl<>(List.of(note1, note2), pageable, 2);
			NoteSearchQuery query = new NoteSearchQuery("spring", pageable);

			given(noteRepository.searchMyNotes(eq(userId), eq("spring"), any(Pageable.class)))
					.willReturn(notePage);

			Page<NoteSummaryResult> result = noteService.searchMyNotes(userId, query);

			assertThat(result.getTotalElements()).isEqualTo(2);
			assertThat(result.getContent()).hasSize(2);
			assertThat(result.getContent().get(0).folderId()).isEqualTo(10L);
			assertThat(result.getContent().get(1).folderId()).isNull();
			verify(noteRepository).searchMyNotes(eq(userId), eq("spring"), any(Pageable.class));
			verifyNoMoreInteractions(noteRepository);
			verifyNoInteractions(userRepository, folderRepository);
		}

		@Test
		@DisplayName("keyword가 비어있으면 NOTE_SEARCH_KEYWORD_REQUIRED 예외가 발생한다")
		void throwsWhenKeywordBlank() {
			Long userId = 1L;
			Pageable pageable = PageRequest.of(0, 10);
			NoteSearchQuery query = new NoteSearchQuery("   ", pageable);

			assertThatThrownBy(() -> noteService.searchMyNotes(userId, query))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_SEARCH_KEYWORD_REQUIRED);

			verifyNoInteractions(noteRepository, userRepository, folderRepository);
		}

		@Test
		@DisplayName("keyword 앞뒤 공백은 trim되어 검색된다")
		void trimsKeywordBeforeSearching() {
			Long userId = 1L;
			User user = user(userId);
			Note note = Note.create(user, null, "spring 제목", "내용", NoteVisibility.PUBLIC, true);
			Pageable pageable = PageRequest.of(0, 10);
			Page<Note> notePage = new PageImpl<>(List.of(note), pageable, 1);
			NoteSearchQuery query = new NoteSearchQuery("  spring  ", pageable);

			given(noteRepository.searchMyNotes(eq(userId), eq("spring"), any(Pageable.class)))
					.willReturn(notePage);

			Page<NoteSummaryResult> result = noteService.searchMyNotes(userId, query);

			assertThat(result.getTotalElements()).isEqualTo(1);
			ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
			verify(noteRepository).searchMyNotes(eq(userId), keywordCaptor.capture(), any(Pageable.class));
			assertThat(keywordCaptor.getValue()).isEqualTo("spring");
			verifyNoMoreInteractions(noteRepository);
			verifyNoInteractions(userRepository, folderRepository);
		}
	}

	@Nested
	@DisplayName("공개 노트 검색")
	class SearchPublicNote {

		@Test
		@DisplayName("keyword가 있으면 공개 검색 결과를 페이지로 반환한다")
		void returnsPageWhenKeywordProvided() {
			User author1 = user(1L);
			User author2 = user(2L);
			Folder folder = folder(author1, 10L);
			Note note1 = Note.create(author1, folder, "spring 제목", "내용", NoteVisibility.PUBLIC, true);
			Note note2 = Note.create(author2, null, "제목", "spring 내용", NoteVisibility.PUBLIC, false);
			Pageable pageable = PageRequest.of(0, 10);
			Page<Note> notePage = new PageImpl<>(List.of(note1, note2), pageable, 2);
			NoteSearchQuery query = new NoteSearchQuery("spring", pageable);

			given(noteRepository.searchPublicNotes(eq("spring"), eq(pageable)))
					.willReturn(notePage);

			Page<NoteSummaryResult> result = noteService.searchPublicNotes(query);

			assertThat(result.getTotalElements()).isEqualTo(2);
			assertThat(result.getContent()).hasSize(2);
			assertThat(result.getContent().get(0).folderId()).isEqualTo(10L);
			assertThat(result.getContent().get(0).visibility()).isEqualTo(NoteVisibility.PUBLIC);
			assertThat(result.getContent().get(1).folderId()).isNull();
			verify(noteRepository).searchPublicNotes(eq("spring"), eq(pageable));
			verifyNoMoreInteractions(noteRepository);
			verifyNoInteractions(userRepository, folderRepository);
		}

		@Test
		@DisplayName("keyword가 비어있으면 NOTE_SEARCH_KEYWORD_REQUIRED 예외가 발생한다")
		void throwsWhenKeywordBlank() {
			Pageable pageable = PageRequest.of(0, 10);
			NoteSearchQuery query = new NoteSearchQuery("   ", pageable);

			assertThatThrownBy(() -> noteService.searchPublicNotes(query))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_SEARCH_KEYWORD_REQUIRED);

			verifyNoInteractions(noteRepository, userRepository, folderRepository);
		}

		@Test
		@DisplayName("keyword 앞뒤 공백은 trim되어 공개 검색된다")
		void trimsKeywordBeforeSearching() {
			User author = user(1L);
			Note note = Note.create(author, null, "spring 제목", "내용", NoteVisibility.PUBLIC, true);
			Pageable pageable = PageRequest.of(0, 10);
			Page<Note> notePage = new PageImpl<>(List.of(note), pageable, 1);
			NoteSearchQuery query = new NoteSearchQuery("  spring  ", pageable);

			given(noteRepository.searchPublicNotes(eq("spring"), eq(pageable)))
					.willReturn(notePage);

			Page<NoteSummaryResult> result = noteService.searchPublicNotes(query);

			assertThat(result.getTotalElements()).isEqualTo(1);

			ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
			ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
			verify(noteRepository).searchPublicNotes(keywordCaptor.capture(), pageableCaptor.capture());
			assertThat(keywordCaptor.getValue()).isEqualTo("spring");
			assertThat(pageableCaptor.getValue()).isEqualTo(pageable);
			verifyNoMoreInteractions(noteRepository);
			verifyNoInteractions(userRepository, folderRepository);
		}
	}

	private Folder folder(User owner, Long folderId) {
		Folder folder = Folder.create(owner, null, DIRECTORY_NAME);
		ReflectionTestUtils.setField(folder, "id", folderId);
		return folder;
	}
}
