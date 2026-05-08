package com.keepgoing.keepgoing.note.domain;

import static com.keepgoing.keepgoing.support.UserFixture.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.keepgoing.keepgoing.folder.domain.Folder;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NoteTest {

	private static final Long AUTHOR_ID = 1L;
	private static final Long OTHER_USER_ID = 2L;
	private static final String TITLE = "제목";
	private static final String CONTENT = "내용";
	private static final String NEW_TITLE = "new title";
	private static final String NEW_CONTENT = "new content";
	private static final String RENAMED_TITLE = "새 제목";
	private static final String BLANK = " ";
	private static final String MAX_LENGTH_TITLE = "a".repeat(200);
	private static final String TOO_LONG_TITLE = "a".repeat(201);

	@Nested
	@DisplayName("create")
	class Create {

		@Test
		@DisplayName("노트 생성 기본값을 세팅한다")
		void setsDefaultValues() {
			// given
			User author = author();

			// when
			Note note = Note.create(author, null, TITLE, CONTENT, null, true);

			// then
			assertSoftly(softly -> {
				softly.assertThat(note.getAuthor()).isEqualTo(author);
				softly.assertThat(note.getFolder()).isNull();
				softly.assertThat(note.getTitle()).isEqualTo(TITLE);
				softly.assertThat(note.getContent()).isEqualTo(CONTENT);
				softly.assertThat(note.getVisibility()).isEqualTo(NoteVisibility.PRIVATE);
				softly.assertThat(note.isAiCollectable()).isTrue();
			});
		}

		@Test
		@DisplayName("visibility가 null이면 PRIVATE으로 생성한다")
		void defaultsVisibilityToPrivateWhenNull() {
			// given
			User author = author();

			// when
			Note note = Note.create(author, null, TITLE, CONTENT, null, false);

			// then
			assertThat(note.getVisibility()).isEqualTo(NoteVisibility.PRIVATE);
		}

		@Test
		@DisplayName("visibility가 주어지면 해당 값으로 생성한다")
		void usesGivenVisibility() {
			// given
			User author = author();

			// when
			Note note = Note.create(author, null, TITLE, CONTENT, NoteVisibility.PUBLIC, false);

			// then
			assertThat(note.getVisibility()).isEqualTo(NoteVisibility.PUBLIC);
		}

		@Test
		@DisplayName("작성자 소유 폴더에 노트를 생성한다")
		void createsNoteInOwnedFolder() {
			// given
			User author = author();
			Folder folder = folderOf(author);

			// when
			Note note = Note.create(author, folder, TITLE, CONTENT, NoteVisibility.PRIVATE, false);

			// then
			assertThat(note.getAuthor()).isEqualTo(author);
			assertThat(note.getFolder()).isEqualTo(folder);
		}

		@Test
		@DisplayName("다른 사용자의 폴더에는 노트를 생성할 수 없다")
		void throwsWhenFolderOwnerDoesNotMatchAuthor() {
			// given
			User author = author();
			Folder otherUserFolder = folderOf(otherUser());

			// when & then
			assertThatThrownBy(() -> Note.create(
					author,
					otherUserFolder,
					TITLE,
					CONTENT,
					NoteVisibility.PRIVATE,
					false
			))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_ACCESS_DENIED);
		}

		@Test
		@DisplayName("작성자 ID가 없으면 노트를 생성할 수 없다")
		void throwsWhenAuthorIdIsNull() {
			// given
			User author = transientAuthor();

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
		@DisplayName("제목과 내용이 null이면 빈 문자열로 생성한다")
		void createsWithEmptyStringsWhenNull() {
			// given
			User author = author();

			// when
			Note note = Note.create(author, null, null, null, null, false);

			// then
			assertSoftly(softly -> {
				softly.assertThat(note.getTitle()).isEqualTo("");
				softly.assertThat(note.getContent()).isEqualTo("");
			});
		}

		@Test
		@DisplayName("PUBLIC으로 노트를 생성할 때 제목이 비어있으면 예외가 발생한다")
		void throwsWhenCreatingPublicNoteWithEmptyTitle() {
			// given
			User author = author();

			// when & then
			assertThatThrownBy(() -> Note.create(author, null, "", "내용", NoteVisibility.PUBLIC, false))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_TITLE_INVALID);
		}
	}

	@Nested
	@DisplayName("update")
	class Update {

		@Test
		@DisplayName("작성자가 제목/내용/visibility/aiCollectable을 변경한다")
		void changesFields() {
			// given
			User author = author();
			Note note = Note.create(author, null, "old title", "old content", NoteVisibility.PRIVATE, true);

			// when
			note.update(author.getId(), NEW_TITLE, NEW_CONTENT, NoteVisibility.PUBLIC, false);

			// then
			assertSoftly(softly -> {
				softly.assertThat(note.getTitle()).isEqualTo(NEW_TITLE);
				softly.assertThat(note.getContent()).isEqualTo(NEW_CONTENT);
				softly.assertThat(note.getVisibility()).isEqualTo(NoteVisibility.PUBLIC);
				softly.assertThat(note.isAiCollectable()).isFalse();
			});
		}

		@Test
		@DisplayName("visibility가 null이면 PRIVATE으로 변경한다")
		void defaultsVisibilityToPrivateWhenNull() {
			// given
			User author = author();
			Note note = Note.create(author, null, TITLE, CONTENT, NoteVisibility.PUBLIC, false);

			// when
			note.update(author.getId(), NEW_TITLE, NEW_CONTENT, null, false);

			// then
			assertThat(note.getVisibility()).isEqualTo(NoteVisibility.PRIVATE);
		}

		@Test
		@DisplayName("수정 시 제목과 내용이 null이면 빈 문자열로 저장한다")
		void updatesWithEmptyStringsWhenNull() {
			// given
			User author = author();
			Note note = Note.create(author, null, TITLE, CONTENT, NoteVisibility.PRIVATE, false);

			// when
			note.update(author.getId(), null, null, null, false);

			// then
			assertSoftly(softly -> {
				softly.assertThat(note.getTitle()).isEqualTo("");
				softly.assertThat(note.getContent()).isEqualTo("");
			});
		}

		@Test
		@DisplayName("제목이 비어있는 상태로 PUBLIC으로 변경하려고 하면 예외가 발생한다")
		void throwsWhenUpdatingToPublicWithEmptyTitle() {
			// given
			User author = author();
			Note note = Note.create(author, null, "", "내용", NoteVisibility.PRIVATE, false);

			// when & then
			assertThatThrownBy(() -> note.update(author.getId(), "", "내용", NoteVisibility.PUBLIC, false))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_TITLE_INVALID);
		}

		@Test
		@DisplayName("본문이 비어있는 상태로 PUBLIC으로 변경하려고 하면 예외가 발생한다")
		void throwsWhenUpdatingToPublicWithEmptyContent() {
			// given
			User author = author();
			Note note = Note.create(author, null, "제목", "", NoteVisibility.PRIVATE, false);

			// when & then
			assertThatThrownBy(() -> note.update(author.getId(), "제목", "", NoteVisibility.PUBLIC, false))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_CONTENT_REQUIRED);
		}

		@Test
		@DisplayName("작성자가 아니면 수정할 수 없다")
		void throwsWhenRequesterIsNotAuthor() {
			// given
			User author = author();
			Note note = note(author);

			// when & then
			assertThatThrownBy(() -> note.update(
					OTHER_USER_ID,
					NEW_TITLE,
					NEW_CONTENT,
					NoteVisibility.PUBLIC,
					false
			))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_ACCESS_DENIED);
		}
	}

	@Nested
	@DisplayName("softDeleteBy")
	class SoftDeleteBy {

		@Test
		@DisplayName("작성자가 삭제하면 deletedAt을 채운다")
		void setsDeletedAt() {
			// given
			User author = author();
			Note note = note(author);

			// when
			note.softDeleteBy(author.getId());

			// then
			assertThat(note.isDeleted()).isTrue();
			assertThat(note.getDeletedAt()).isNotNull();
		}

		@Test
		@DisplayName("여러 번 삭제해도 최초 삭제 시각을 유지한다")
		void preservesDeletedAtWhenAlreadyDeleted() {
			// given
			User author = author();
			Note note = note(author);

			// when
			note.softDeleteBy(author.getId());
			var firstDeletedAt = note.getDeletedAt();
			assertThat(firstDeletedAt).isNotNull();
			note.softDeleteBy(author.getId());

			// then
			assertThat(note.getDeletedAt()).isEqualTo(firstDeletedAt);
		}

		@Test
		@DisplayName("작성자가 아니면 삭제할 수 없다")
		void throwsWhenRequesterIsNotAuthor() {
			// given
			Note note = note(author());

			// when & then
			assertThatThrownBy(() -> note.softDeleteBy(OTHER_USER_ID))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_ACCESS_DENIED);
		}
	}

	@Nested
	@DisplayName("changeFolder")
	class ChangeFolder {

		@Test
		@DisplayName("작성자가 요청하고 같은 소유자의 폴더면 이동한다")
		void movesWhenRequesterIsAuthorAndFolderOwnerMatches() {
			// given
			User author = author();
			Folder targetFolder = folderOf(author);
			Note note = note(author);

			// when
			note.changeFolder(author.getId(), targetFolder);

			// then
			assertThat(note.getFolder()).isEqualTo(targetFolder);
		}

		@Test
		@DisplayName("작성자가 요청하면 루트로 이동할 수 있다")
		void movesToRootWhenTargetFolderIsNull() {
			// given
			User author = author();
			Folder currentFolder = folderOf(author);
			Note note = note(author, currentFolder);

			// when
			note.changeFolder(author.getId(), null);

			// then
			assertThat(note.getFolder()).isNull();
		}

		@Test
		@DisplayName("작성자가 아니면 폴더를 이동할 수 없다")
		void throwsWhenRequesterIsNotAuthor() {
			// given
			User author = author();
			Folder targetFolder = folderOf(author);
			Note note = note(author);

			// when & then
			assertThatThrownBy(() -> note.changeFolder(OTHER_USER_ID, targetFolder))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_ACCESS_DENIED);
		}

		@Test
		@DisplayName("다른 사용자의 폴더로는 이동할 수 없다")
		void throwsWhenTargetFolderOwnedByAnotherUser() {
			// given
			User author = author();
			Folder otherFolder = folderOf(otherUser());
			Note note = note(author);

			// when & then
			assertThatThrownBy(() -> note.changeFolder(author.getId(), otherFolder))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_ACCESS_DENIED);
		}
	}

	@Nested
	@DisplayName("renameTitle")
	class RenameTitle {

		@Test
		@DisplayName("작성자가 요청하면 제목을 변경한다")
		void changesTitleWhenRequesterIsAuthor() {
			// given
			User author = author();
			Note note = note(author);

			// when
			note.renameTitle(author.getId(), RENAMED_TITLE);

			// then
			assertThat(note.getTitle()).isEqualTo(RENAMED_TITLE);
		}

		@Test
		@DisplayName("작성자가 아니면 제목을 변경할 수 없다")
		void throwsWhenRequesterIsNotAuthor() {
			// given
			Note note = note(author());

			// when & then
			assertThatThrownBy(() -> note.renameTitle(OTHER_USER_ID, RENAMED_TITLE))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_ACCESS_DENIED);
		}

		@Test
		@DisplayName("제목을 공백으로 바꿀 수 없다")
		void throwsWhenTitleIsBlank() {
			// given
			User author = author();
			Note note = note(author);

			// when & then
			assertThatThrownBy(() -> note.renameTitle(author.getId(), BLANK))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_TITLE_INVALID);
		}

		@Test
		@DisplayName("제목이 길이 제한을 넘으면 변경할 수 없다")
		void throwsWhenTitleIsTooLong() {
			// given
			User author = author();
			Note note = note(author);

			// when & then
			assertThatThrownBy(() -> note.renameTitle(author.getId(), TOO_LONG_TITLE))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_TITLE_INVALID);
		}

		@Test
		@DisplayName("제목을 null로 변경할 수 없다")
		void throwsWhenTitleIsNull() {
			// given
			User author = author();
			Note note = note(author);

			// when & then
			assertThatThrownBy(() -> note.renameTitle(author.getId(), null))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_TITLE_INVALID);
		}

		@Test
		@DisplayName("제목 길이가 200자면 변경할 수 있다")
		void changesTitleWhenLengthIsExactly200() {
			// given
			User author = author();
			Note note = note(author);

			// when
			note.renameTitle(author.getId(), MAX_LENGTH_TITLE);

			// then
			assertThat(note.getTitle()).isEqualTo(MAX_LENGTH_TITLE);
		}
	}

	@Nested
	@DisplayName("queries")
	class Queries {

		@Test
		@DisplayName("isAuthor는 작성자 여부를 반환한다")
		void isAuthorReturnsTrueOnlyForAuthor() {
			// given
			Note note = note(author());

			// expect
			assertThat(note.isAuthor(AUTHOR_ID)).isTrue();
			assertThat(note.isAuthor(OTHER_USER_ID)).isFalse();
		}

		@Test
		@DisplayName("getAuthorId는 작성자 ID를 반환한다")
		void getAuthorIdReturnsAuthorId() {
			// given
			Note note = note(author());

			// when & then
			assertThat(note.getAuthorId()).isEqualTo(AUTHOR_ID);
		}
	}

	private static User author() {
		return user(AUTHOR_ID, "author@test.com", "author");
	}

	private static User otherUser() {
		return user(OTHER_USER_ID, "other@test.com", "other");
	}

	private static User transientAuthor() {
		return User.create("author@test.com", "author");
	}

	private static Folder folderOf(User owner) {
		return Folder.create(owner, null, "backend");
	}

	private static Note note(User author) {
		return note(author, null);
	}

	private static Note note(User author, Folder folder) {
		return Note.create(author, folder, TITLE, CONTENT, NoteVisibility.PRIVATE, false);
	}
}
