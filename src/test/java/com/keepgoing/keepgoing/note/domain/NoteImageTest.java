package com.keepgoing.keepgoing.note.domain;

import static com.keepgoing.keepgoing.support.UserFixture.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.user.domain.User;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

class NoteImageTest {

	private static final String STORAGE_KEY = "note-images/2026/04/image.png";
	private static final String ORIGINAL_NAME = "image.png";
	private static final String CONTENT_TYPE = "image/png";
	private static final Long FILE_SIZE = 1024L;
	private static final String NOTE_TITLE = "노트 제목";
	private static final String NOTE_CONTENT = "노트 본문";

	@Nested
	@DisplayName("create")
	class Create {

		@Test
		@DisplayName("노트 작성자는 유효한 이미지 메타데이터로 노트 이미지를 생성할 수 있다")
		void create_createsNoteImageWhenUploaderIsNoteAuthor() {
			// given
			User uploader = user(1L);
			Note note = note(uploader);

			// when
			NoteImage noteImage = NoteImage.create(
					note,
					uploader,
					STORAGE_KEY,
					ORIGINAL_NAME,
					CONTENT_TYPE,
					FILE_SIZE
			);

			// then
			assertThat(noteImage.getId()).isNull();
			assertThat(noteImage.getPublicId()).isNotNull();
			assertThat(noteImage.getNote()).isEqualTo(note);
			assertThat(noteImage.getUploader()).isEqualTo(uploader);
			assertThat(noteImage.getStorageKey()).isEqualTo(STORAGE_KEY);
			assertThat(noteImage.getOriginalName()).isEqualTo(ORIGINAL_NAME);
			assertThat(noteImage.getContentType()).isEqualTo(CONTENT_TYPE);
			assertThat(noteImage.getFileSize()).isEqualTo(FILE_SIZE);
			assertThat(noteImage.getDeletedAt()).isNull();
		}

		@Test
		@DisplayName("노트가 없으면 생성할 수 없다")
		void create_throwsWhenNoteIsNull() {
			// given
			User uploader = user(1L);

			// when & then
			assertThatThrownBy(() -> NoteImage.create(
					nullNote(),
					uploader,
					STORAGE_KEY,
					ORIGINAL_NAME,
					CONTENT_TYPE,
					FILE_SIZE
			))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
		}

		@Test
		@DisplayName("저장되지 않은 노트에는 이미지를 생성할 수 없다")
		void create_throwsWhenNoteIsNotPersisted() {
			// given
			User uploader = user(1L);
			Note unsavedNote = unsavedNote(uploader);

			// when & then
			assertThatThrownBy(() -> NoteImage.create(
					unsavedNote,
					uploader,
					STORAGE_KEY,
					ORIGINAL_NAME,
					CONTENT_TYPE,
					FILE_SIZE
			))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
		}

		@Test
		@DisplayName("삭제된 노트에는 이미지를 생성할 수 없다")
		void create_throwsWhenNoteIsDeleted() {
			// given
			User uploader = user(1L);
			Note note = note(uploader);
			note.softDelete();

			// when & then
			assertThatThrownBy(() -> NoteImage.create(
					note,
					uploader,
					STORAGE_KEY,
					ORIGINAL_NAME,
					CONTENT_TYPE,
					FILE_SIZE
			))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
		}

		@Test
		@DisplayName("업로더가 없으면 생성할 수 없다")
		void create_throwsWhenUploaderIsNull() {
			// given
			Note note = note(user(1L));

			// when & then
			assertThatThrownBy(() -> NoteImage.create(
					note,
					null,
					STORAGE_KEY,
					ORIGINAL_NAME,
					CONTENT_TYPE,
					FILE_SIZE
			))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INTERNAL_SERVER_ERROR);
		}

		@Test
		@DisplayName("업로더 ID가 없으면 생성할 수 없다")
		void create_throwsWhenUploaderIdIsNull() {
			// given
			User author = user(1L);
			Note note = note(author);
			User uploader = user(null);

			// when & then
			assertThatThrownBy(() -> NoteImage.create(
					note,
					uploader,
					STORAGE_KEY,
					ORIGINAL_NAME,
					CONTENT_TYPE,
					FILE_SIZE
			))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INTERNAL_SERVER_ERROR);
		}

		@Test
		@DisplayName("업로더가 노트 작성자가 아니면 생성할 수 없다")
		void create_throwsWhenUploaderIsNotNoteAuthor() {
			// given
			User author = user(1L);
			User uploader = user(2L, "uploader@test.com", "uploader");
			Note note = note(author);

			// when & then
			assertThatThrownBy(() -> NoteImage.create(
					note,
					uploader,
					STORAGE_KEY,
					ORIGINAL_NAME,
					CONTENT_TYPE,
					FILE_SIZE
			))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_IMAGE_ACCESS_DENIED);
		}

		@ParameterizedTest(name = "[{index}] {0}")
		@MethodSource("invalidMetadataCases")
		@DisplayName("유효하지 않은 메타데이터로는 노트 이미지를 생성할 수 없다")
		void create_throwsWhenMetadataIsInvalid(
				String label,
				String storageKey,
				String originalName,
				String contentType,
				Long fileSize
		) {
			// given
			User uploader = user(1L);
			Note note = note(uploader);

			// when & then
			assertThatThrownBy(() -> NoteImage.create(
					note,
					uploader,
					storageKey,
					originalName,
					contentType,
					fileSize
			))
					.as("invalid metadata case: %s", label)
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
		}

		static Stream<Arguments> invalidMetadataCases() {
			return Stream.of(
					Arguments.of("storageKey가 공백", "   ", ORIGINAL_NAME, CONTENT_TYPE, FILE_SIZE),
					Arguments.of("storageKey가 512자 초과", "a".repeat(513), ORIGINAL_NAME, CONTENT_TYPE, FILE_SIZE),
					Arguments.of("originalName이 공백", STORAGE_KEY, "   ", CONTENT_TYPE, FILE_SIZE),
					Arguments.of("originalName이 255자 초과", STORAGE_KEY, "a".repeat(256), CONTENT_TYPE, FILE_SIZE),
					Arguments.of("contentType이 공백", STORAGE_KEY, ORIGINAL_NAME, "   ", FILE_SIZE),
					Arguments.of("contentType이 100자 초과", STORAGE_KEY, ORIGINAL_NAME, "a".repeat(101), FILE_SIZE),
					Arguments.of("fileSize가 null", STORAGE_KEY, ORIGINAL_NAME, CONTENT_TYPE, null),
					Arguments.of("fileSize가 0", STORAGE_KEY, ORIGINAL_NAME, CONTENT_TYPE, 0L),
					Arguments.of("fileSize가 음수", STORAGE_KEY, ORIGINAL_NAME, CONTENT_TYPE, -1L)
			);
		}

		private static Note nullNote() {
			return null;
		}
	}

	@Nested
	@DisplayName("uploader")
	class Uploader {

		@Test
		@DisplayName("업로더가 일치하면 검증을 통과한다")
		void validateUploader_passesWhenUploaderMatches() {
			// given
			NoteImage noteImage = noteImage(user(1L));

			// when & then
			assertThatCode(() -> noteImage.validateUploader(1L))
					.doesNotThrowAnyException();
			assertThat(noteImage.isUploader(1L)).isTrue();
		}

		@Test
		@DisplayName("업로더가 일치하지 않으면 검증에 실패한다")
		void validateUploader_throwsWhenUploaderDoesNotMatch() {
			// given
			NoteImage noteImage = noteImage(user(1L));

			// when & then
			assertThatThrownBy(() -> noteImage.validateUploader(2L))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_IMAGE_ACCESS_DENIED);
			assertThat(noteImage.isUploader(2L)).isFalse();
		}

		@Test
		@DisplayName("요청 사용자 ID가 없으면 업로더가 아니다")
		void isUploader_returnsFalseWhenUserIdIsNull() {
			// given
			NoteImage noteImage = noteImage(user(1L));

			// when & then
			assertThat(noteImage.isUploader(null)).isFalse();
		}
	}

	@Nested
	@DisplayName("softDelete")
	class SoftDelete {

		@Test
		@DisplayName("삭제 시각을 설정한다")
		void softDelete_marksNoteImageAsDeleted() {
			// given
			NoteImage noteImage = noteImage(user(1L));

			// when
			noteImage.softDelete();

			// then
			assertThat(noteImage.getDeletedAt()).isNotNull();
		}

		@Test
		@DisplayName("이미 삭제된 이미지를 다시 삭제해도 삭제 시각을 유지한다")
		void softDelete_keepsDeletedAtWhenAlreadyDeleted() {
			// given
			NoteImage noteImage = noteImage(user(1L));
			noteImage.softDelete();
			var firstDeletedAt = noteImage.getDeletedAt();

			// when
			noteImage.softDelete();

			// then
			assertThat(noteImage.getDeletedAt()).isEqualTo(firstDeletedAt);
		}
	}

	private NoteImage noteImage(User uploader) {
		return NoteImage.create(
				note(uploader),
				uploader,
				STORAGE_KEY,
				ORIGINAL_NAME,
				CONTENT_TYPE,
				FILE_SIZE
		);
	}

	private Note note(User author) {
		Note note = unsavedNote(author);
		ReflectionTestUtils.setField(note, "id", 1L);
		return note;
	}

	private Note unsavedNote(User author) {
		return Note.create(
				author,
				null,
				NOTE_TITLE,
				NOTE_CONTENT,
				NoteVisibility.PRIVATE,
				false
		);
	}
}
