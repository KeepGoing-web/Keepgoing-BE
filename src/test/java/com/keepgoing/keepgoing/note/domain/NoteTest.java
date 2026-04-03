package com.keepgoing.keepgoing.note.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.keepgoing.keepgoing.folder.domain.Folder;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NoteTest {

	public static final String DIR_NAME = "backend";
	public static final String TITLE = "제목";
	public static final String CONTENT = "내용";

	private User createUser(Long id, String email, String name) {
		return User.builder()
				.id(id)
				.email(email)
				.name(name)
				.build();
	}

	// ========== create ==========
	@Nested
	@DisplayName("create")
	class Create {
		@Test
		@DisplayName("기본값이 잘 세팅 되는지 테스트")
		void create_setsDefault() {
			// given
			User author = User.builder()
					.id(1L)
					.build();

			String title = TITLE;
			String content = CONTENT;

			// when
			Note note = Note.create(
					author,
					null,
					title,
					content,
					null,
					true
			);

			// then
			assertThat(note.getAuthor()).isEqualTo(author);
			assertThat(note.getFolder()).isNull();
			assertThat(note.getTitle()).isEqualTo(title);
			assertThat(note.getContent()).isEqualTo(content);
			assertThat(note.getVisibility()).isEqualTo(NoteVisibility.PRIVATE); // null일 때 기본값
			assertThat(note.isAiCollectable()).isTrue();
		}

		@Test
		@DisplayName("visibility가 null이면 PRIVATE 기본값")
		void create_defaultVisibilityWhenNull() {
			//given
			User author = User.builder()
					.id(1L)
					.email("test@example.com")
					.name("테스트유저")
					.build();

			String title = TITLE;
			String content = CONTENT;
			boolean aiCollectable = false;

			//when
			Note note = Note.create(
					author,
					null,
					title,
					content,
					null,
					aiCollectable
			);

			//then
			assertThat(note.getAuthorId()).isEqualTo(1L);
			assertThat(note.getTitle()).isEqualTo(title);
			assertThat(note.getContent()).isEqualTo(content);
			assertThat(note.getVisibility()).isEqualTo(NoteVisibility.PRIVATE);
			assertThat(note.isAiCollectable()).isFalse();
		}

		@Test
		@DisplayName("visibility가 주어지면 그 값을 사용")
		void create_useGivenVisibility() {
			//given
			User author = User.builder()
					.id(1L)
					.email("test@example.com")
					.name("테스트유저")
					.build();

			String title = TITLE;
			String content = CONTENT;
			NoteVisibility visibility = NoteVisibility.PUBLIC;
			boolean aiCollectable = false;

			//when
			Note note = Note.create(author, null, title, content, visibility, aiCollectable);

			//then
			assertThat(note.getVisibility()).isEqualTo(NoteVisibility.PUBLIC);
			assertThat(note.isAiCollectable()).isFalse();
		}

		@Test
		@DisplayName("폴더 owner와 작성자가 같으면 해당 폴더로 노트를 생성한다.")
		void create_createsNoteWhenFolderOwnerMatchesAuthor() {
			// given
			User author = createUser(1L, "test@example.com", "test");
			Folder folder = Folder.create(author, null, DIR_NAME);

			// when
			Note note = Note.create(
					author,
					folder,
					TITLE,
					CONTENT,
					NoteVisibility.PRIVATE,
					false
			);

			// then
			assertThat(note.getAuthor()).isEqualTo(author);
			assertThat(note.getFolder()).isEqualTo(folder);
			assertThat(note.getTitle()).isEqualTo(TITLE);
			assertThat(note.getContent()).isEqualTo(CONTENT);
			assertThat(note.getVisibility()).isEqualTo(NoteVisibility.PRIVATE);
			assertThat(note.isAiCollectable()).isFalse();
		}

		@Test
		@DisplayName("폴더 owner와 작성자가 다르면 FOLDER_ACCESS_DENIED 예외가 발생한다.")
		void create_throwsWhenFolderOwnerDoesNotMatchAuthor() {
			// given
			Long authorId = 1L;
			User author = createUser(authorId, "test@test.com", "test");

			Long otherAuthorId = 2L;
			User otherAuthor = createUser(otherAuthorId, "other@other.com", "other");

			Folder otherUserFolder = Folder.create(otherAuthor, null, DIR_NAME);

			// when & then
			assertThatThrownBy(() ->
			{
				Note.create(
						author,
						otherUserFolder,
						TITLE,
						CONTENT,
						NoteVisibility.PRIVATE,
						false
				);
			})
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_ACCESS_DENIED);
		}

		@Test
		@DisplayName("작성자 ID가 없으면 INTERNAL_SERVER_ERROR 예외가 발생한다.")
		void create_throwsWhenAuthorIdIsNull() {
			// given
			User author = User.create("test@test.com", "test");

			// when & then
			assertThatThrownBy(() -> Note.create(
					author,
					null,
					TITLE,
					CONTENT,
					NoteVisibility.PRIVATE,
					false
			))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INTERNAL_SERVER_ERROR);
		}

		@Test
		@DisplayName("제목이 비어있으면 INVALID_INPUT 예외가 발생한다.")
		void create_throwsWhenTitleIsBlank() {
			// given
			User author = createUser(1L, "test@test.com", "test");

			// when & then
			assertThatThrownBy(() -> Note.create(
					author,
					null,
					"   ",
					CONTENT,
					NoteVisibility.PRIVATE,
					false
			))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
		}

		@Test
		@DisplayName("본문이 비어있으면 INVALID_INPUT 예외가 발생한다.")
		void create_throwsWhenContentIsBlank() {
			// given
			User author = createUser(1L, "test@test.com", "test");

			// when & then
			assertThatThrownBy(() -> Note.create(
					author,
					null,
					TITLE,
					"   ",
					NoteVisibility.PRIVATE,
					false
			))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
		}

	}

	// ========== update ==========
	@Test
	@DisplayName("update: 제목/내용/visibility/aiCollectable 변경")
	void update_changeFields() {
		// given
		User author = User.builder()
				.id(1L)
				.build();

		Note note = Note.create(
				author,
				null,
				"old title",
				"old content",
				NoteVisibility.PRIVATE,
				true
		);

		// when
		note.update(
				"new title",
				"new content",
				NoteVisibility.PUBLIC,
				false);

		// then
		assertThat(note.getTitle()).isEqualTo("new title");
		assertThat(note.getContent()).isEqualTo("new content");
		assertThat(note.getVisibility()).isEqualTo(NoteVisibility.PUBLIC);
		assertThat(note.isAiCollectable()).isFalse();
	}

	// ========== softDelete ==========
	@Test
	@DisplayName("softDelete: deletedAt 채워지고 isDeleted true 반환")
	void softDelete_setsDeletedAt() {
		// given
		User author = User.builder()
				.id(1L)
				.email("test@example.com")
				.name("테스트유저")
				.build();

		Note note = Note.create(
				author,
				null,
				"title",
				"content",
				NoteVisibility.PRIVATE,
				true
		);

		// when
		note.softDelete();

		// then
		assertThat(note.isDeleted()).isTrue();
		assertThat(note.getDeletedAt()).isNotNull();
	}

	// ========== validateAuthor ==========
	@Test
	@DisplayName("validateAuthor: 작성자가 아니면 예외 발생")
	void validateAuthor_throws_whenNotAuthor() {
		// given
		User author = User.builder()
				.id(1L)
				.email("a@a.com")
				.name("작성자")
				.build();

		Note note = Note.create(
				author,
				null,
				"title",
				"content",
				NoteVisibility.PRIVATE,
				true
		);

		// when & then
		assertThatThrownBy(() -> note.validateAuthor(2L))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_ACCESS_DENIED);
	}

	// ========== isAuthor ==========
	@Test
	@DisplayName("isAuthor: 작성자인지 여부를 반환")
	void isAuthor_returnsTrueOnlyForAuthor() {
		// given
		User author = User.builder()
				.id(1L)
				.email("a@a.com")
				.name("작성자")
				.build();

		Note note = Note.create(
				author,
				null,
				"title",
				"content",
				NoteVisibility.PRIVATE,
				true
		);

		// expect
		assertThat(note.isAuthor(1L)).isTrue();
		assertThat(note.isAuthor(2L)).isFalse();
	}

	// ========== getAuthorId ==========
	@Test
	@DisplayName("getAuthorId: 작성자 ID를 정상적으로 반환")
	void getAuthorId_returnsAuthorId() {
		// given
		User author = User.builder()
				.id(1L)
				.build();

		Note note = Note.create(
				author,
				null,
				"title",
				"content",
				NoteVisibility.PRIVATE,
				true);

		// when & then
		assertThat(note.getAuthorId()).isEqualTo(1L);
	}

	// ========== changeFolder ==========
	@Test
	@DisplayName("changeFolder: 작성자가 요청하고 같은 소유자의 폴더면 이동한다")
	void changeFolder_movesWhenRequesterIsAuthorAndFolderOwnerMatches() {
		// given
		User author = createUser(1L, "author@test.com", "author");
		Folder targetFolder = Folder.create(author, null, DIR_NAME);
		Note note = Note.create(author, null, TITLE, CONTENT, NoteVisibility.PRIVATE, false);

		// when
		note.changeFolder(author.getId(), targetFolder);

		// then
		assertThat(note.getFolder()).isEqualTo(targetFolder);
	}

	@Test
	@DisplayName("changeFolder: 작성자가 요청하면 루트로 이동할 수 있다")
	void changeFolder_movesToRootWhenTargetFolderIsNull() {
		// given
		User author = createUser(1L, "author@test.com", "author");
		Folder currentFolder = Folder.create(author, null, DIR_NAME);
		Note note = Note.create(author, currentFolder, TITLE, CONTENT, NoteVisibility.PRIVATE, false);

		// when
		note.changeFolder(author.getId(), null);

		// then
		assertThat(note.getFolder()).isNull();
	}

	@Test
	@DisplayName("changeFolder: 작성자가 아니면 NOTE_ACCESS_DENIED 예외가 발생한다")
	void changeFolder_throwsWhenRequesterIsNotAuthor() {
		// given
		User author = createUser(1L, "author@test.com", "author");
		Folder targetFolder = Folder.create(author, null, DIR_NAME);
		Note note = Note.create(author, null, TITLE, CONTENT, NoteVisibility.PRIVATE, false);

		// when & then
		assertThatThrownBy(() -> note.changeFolder(2L, targetFolder))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_ACCESS_DENIED);
	}

	@Test
	@DisplayName("changeFolder: 다른 사용자의 폴더면 FOLDER_ACCESS_DENIED 예외가 발생한다")
	void changeFolder_throwsWhenTargetFolderOwnedByAnotherUser() {
		// given
		User author = createUser(1L, "author@test.com", "author");
		User otherAuthor = createUser(2L, "other@test.com", "other");
		Folder otherFolder = Folder.create(otherAuthor, null, DIR_NAME);
		Note note = Note.create(author, null, TITLE, CONTENT, NoteVisibility.PRIVATE, false);

		// when & then
		assertThatThrownBy(() -> note.changeFolder(author.getId(), otherFolder))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_ACCESS_DENIED);
	}
}
