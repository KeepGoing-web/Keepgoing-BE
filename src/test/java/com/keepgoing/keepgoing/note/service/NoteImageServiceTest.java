package com.keepgoing.keepgoing.note.service;

import static com.keepgoing.keepgoing.support.UserFixture.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.global.storage.InputStreamSupplier;
import com.keepgoing.keepgoing.global.storage.ObjectStorageClient;
import com.keepgoing.keepgoing.global.storage.ObjectStorageException;
import com.keepgoing.keepgoing.note.domain.Note;
import com.keepgoing.keepgoing.note.domain.NoteImage;
import com.keepgoing.keepgoing.note.domain.NoteImageStatus;
import com.keepgoing.keepgoing.note.domain.NoteVisibility;
import com.keepgoing.keepgoing.note.repository.NoteImageRepository;
import com.keepgoing.keepgoing.note.repository.NoteRepository;
import com.keepgoing.keepgoing.note.service.dto.NoteImageUploadCommand;
import com.keepgoing.keepgoing.note.service.dto.NoteImageUploadResult;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class NoteImageServiceTest {

	private static final Long UPLOADER_ID = 1L;
	private static final Long NOTE_ID = 10L;
	private static final String ORIGINAL_FILE_NAME = "image.png";
	private static final String CONTENT_TYPE = "image/png";
	private static final long FILE_SIZE = 1024L;
	private static final String STORAGE_KEY = "notes/10/generated-image-key.png";

	@Mock
	NoteRepository noteRepository;

	@Mock
	NoteImageRepository noteImageRepository;

	@Mock
	UserRepository userRepository;

	@Mock
	ObjectStorageClient objectStorageClient;

	@Mock
	TransactionTemplate transactionTemplate;

	@InjectMocks
	NoteImageService noteImageService;

	@Nested
	@DisplayName("uploadImage")
	class UploadImage {

		@Test
		@DisplayName("유효한 업로드 요청이면 스토리지와 DB를 거쳐 publicId/status를 반환한다")
		void uploadsAndSavesImageSuccessfully() {
			// given
			Note note = persistedNote(user(UPLOADER_ID));
			User uploader = user(UPLOADER_ID);
			NoteImageUploadCommand command = uploadCommand(NOTE_ID);

			given(noteRepository.findById(NOTE_ID)).willReturn(Optional.of(note));
			given(objectStorageClient.upload(
					eq(command.inputStreamSupplier()),
					eq("notes/" + NOTE_ID),
					eq(command.originalFileName()),
					eq(command.fileSize())
			)).willReturn(STORAGE_KEY);
			given(transactionTemplate.execute(any())).willAnswer(invocation -> {
				TransactionCallback<NoteImageUploadResult> callback = invocation.getArgument(0);
				return callback.doInTransaction(mock(TransactionStatus.class));
			});
			given(userRepository.getReferenceById(UPLOADER_ID)).willReturn(uploader);
			given(noteImageRepository.save(any(NoteImage.class))).willAnswer(invocation -> invocation.getArgument(0));

			// when
			NoteImageUploadResult result = noteImageService.uploadImage(UPLOADER_ID, command);

			// then
			ArgumentCaptor<NoteImage> noteImageCaptor = ArgumentCaptor.forClass(NoteImage.class);
			InOrder inOrder = inOrder(objectStorageClient, noteImageRepository);
			inOrder.verify(objectStorageClient).upload(
					eq(command.inputStreamSupplier()),
					eq("notes/" + NOTE_ID),
					eq(command.originalFileName()),
					eq(command.fileSize())
			);
			inOrder.verify(noteImageRepository).save(noteImageCaptor.capture());

			NoteImage savedImage = noteImageCaptor.getValue();
			assertThat(savedImage.getStorageKey()).isEqualTo(STORAGE_KEY);
			assertThat(savedImage.getOriginalName()).isEqualTo(ORIGINAL_FILE_NAME);
			assertThat(savedImage.getContentType()).isEqualTo(CONTENT_TYPE);
			assertThat(savedImage.getFileSize()).isEqualTo(FILE_SIZE);
			assertThat(savedImage.getStatus()).isEqualTo(NoteImageStatus.PENDING);

			assertThat(result.publicId()).isEqualTo(savedImage.getPublicId());
			assertThat(result.status()).isEqualTo(savedImage.getStatus());

			verify(noteRepository).findById(NOTE_ID);
			verify(transactionTemplate).execute(any());
			verify(userRepository).getReferenceById(UPLOADER_ID);
			verify(objectStorageClient, never()).delete(any());
		}

		@Test
		@DisplayName("존재하지 않는 노트에 업로드하면 NOTE_NOT_FOUND 예외를 던지고 업로드를 시도하지 않는다")
		void throwsWhenNoteNotFound() {
			// given
			NoteImageUploadCommand command = uploadCommand(NOTE_ID);
			given(noteRepository.findById(NOTE_ID)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> noteImageService.uploadImage(UPLOADER_ID, command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_NOT_FOUND);

			verify(noteRepository).findById(NOTE_ID);
			verifyNoInteractions(objectStorageClient, transactionTemplate, userRepository, noteImageRepository);
		}

		@Test
		@DisplayName("타인 노트에 업로드하면 NOTE_ACCESS_DENIED 예외를 던지고 업로드를 시도하지 않는다")
		void throwsWhenRequesterIsNotOwner() {
			// given
			NoteImageUploadCommand command = uploadCommand(NOTE_ID);
			Note otherUsersNote = persistedNote(user(2L));
			given(noteRepository.findById(NOTE_ID)).willReturn(Optional.of(otherUsersNote));

			// when & then
			assertThatThrownBy(() -> noteImageService.uploadImage(UPLOADER_ID, command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_ACCESS_DENIED);

			verify(noteRepository).findById(NOTE_ID);
			verifyNoInteractions(objectStorageClient, transactionTemplate, userRepository, noteImageRepository);
		}

		@Test
		@DisplayName("스토리지 업로드가 실패하면 SERVICE_UNAVAILABLE 예외를 던지고 DB 저장을 시도하지 않는다")
		void throwsWhenStorageUploadFails() {
			// given
			Note note = persistedNote(user(UPLOADER_ID));
			NoteImageUploadCommand command = uploadCommand(NOTE_ID);
			ObjectStorageException storageException = new ObjectStorageException("upload failed", new RuntimeException("io"));

			given(noteRepository.findById(NOTE_ID)).willReturn(Optional.of(note));
			given(objectStorageClient.upload(any(), eq("notes/" + NOTE_ID), eq(ORIGINAL_FILE_NAME), eq(FILE_SIZE)))
					.willThrow(storageException);

			// when & then
			assertThatThrownBy(() -> noteImageService.uploadImage(UPLOADER_ID, command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SERVICE_UNAVAILABLE)
					.hasCause(storageException);

			verify(noteRepository).findById(NOTE_ID);
			verify(objectStorageClient).upload(any(), eq("notes/" + NOTE_ID), eq(ORIGINAL_FILE_NAME), eq(FILE_SIZE));
			verifyNoInteractions(transactionTemplate, userRepository, noteImageRepository);
			verify(objectStorageClient, never()).delete(any());
		}

		@Test
		@DisplayName("DB 저장이 실패하면 업로드된 파일을 삭제하고 원래 예외를 그대로 던진다")
		void cleansUpAndRethrowsWhenDbSaveFails() {
			// given
			Note note = persistedNote(user(UPLOADER_ID));
			User uploader = user(UPLOADER_ID);
			NoteImageUploadCommand command = uploadCommand(NOTE_ID);
			RuntimeException dbException = new RuntimeException("db save failed");

			given(noteRepository.findById(NOTE_ID)).willReturn(Optional.of(note));
			given(objectStorageClient.upload(any(), eq("notes/" + NOTE_ID), eq(ORIGINAL_FILE_NAME), eq(FILE_SIZE)))
					.willReturn(STORAGE_KEY);
			given(transactionTemplate.execute(any())).willAnswer(invocation -> {
				TransactionCallback<NoteImageUploadResult> callback = invocation.getArgument(0);
				return callback.doInTransaction(mock(TransactionStatus.class));
			});
			given(userRepository.getReferenceById(UPLOADER_ID)).willReturn(uploader);
			given(noteImageRepository.save(any(NoteImage.class))).willThrow(dbException);

			// when & then
			assertThatThrownBy(() -> noteImageService.uploadImage(UPLOADER_ID, command))
					.isSameAs(dbException);

			InOrder inOrder = inOrder(objectStorageClient, noteImageRepository);
			inOrder.verify(objectStorageClient).upload(any(), eq("notes/" + NOTE_ID), eq(ORIGINAL_FILE_NAME), eq(FILE_SIZE));
			inOrder.verify(noteImageRepository).save(any(NoteImage.class));
			verify(objectStorageClient).delete(STORAGE_KEY);
		}

		@Test
		@DisplayName("DB 저장과 파일 삭제가 모두 실패하면 원래 예외를 던지고 삭제 실패 예외를 suppressed에 보관한다")
		void keepsSuppressedExceptionWhenCleanupAlsoFails() {
			// given
			Note note = persistedNote(user(UPLOADER_ID));
			User uploader = user(UPLOADER_ID);
			NoteImageUploadCommand command = uploadCommand(NOTE_ID);
			RuntimeException dbException = new RuntimeException("db save failed");
			ObjectStorageException deleteException = new ObjectStorageException("delete failed", new RuntimeException("network"));

			given(noteRepository.findById(NOTE_ID)).willReturn(Optional.of(note));
			given(objectStorageClient.upload(any(), eq("notes/" + NOTE_ID), eq(ORIGINAL_FILE_NAME), eq(FILE_SIZE)))
					.willReturn(STORAGE_KEY);
			given(transactionTemplate.execute(any())).willAnswer(invocation -> {
				TransactionCallback<NoteImageUploadResult> callback = invocation.getArgument(0);
				return callback.doInTransaction(mock(TransactionStatus.class));
			});
			given(userRepository.getReferenceById(UPLOADER_ID)).willReturn(uploader);
			given(noteImageRepository.save(any(NoteImage.class))).willThrow(dbException);
			willThrow(deleteException).given(objectStorageClient).delete(STORAGE_KEY);

			// when
			Throwable thrown = catchThrowable(() -> noteImageService.uploadImage(UPLOADER_ID, command));

			// then
			assertThat(thrown).isSameAs(dbException);
			assertThat(thrown.getSuppressed()).hasSize(1);
			assertThat(thrown.getSuppressed()[0]).isSameAs(deleteException);
			verify(objectStorageClient).delete(STORAGE_KEY);
		}
	}

	private static NoteImageUploadCommand uploadCommand(Long noteId) {
		InputStreamSupplier supplier = () -> new ByteArrayInputStream("img".getBytes(StandardCharsets.UTF_8));
		return new NoteImageUploadCommand(
				noteId,
				supplier,
				ORIGINAL_FILE_NAME,
				CONTENT_TYPE,
				FILE_SIZE
		);
	}

	private static Note persistedNote(User author) {
		Note note = Note.create(author, null, "제목", "본문", NoteVisibility.PRIVATE, false);
		ReflectionTestUtils.setField(note, "id", NOTE_ID);
		return note;
	}
}
