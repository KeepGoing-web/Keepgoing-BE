package com.keepgoing.keepgoing.note.service;

import static com.keepgoing.keepgoing.support.ImageProcessingResultEventFixture.pendingEvent;
import static com.keepgoing.keepgoing.support.ImageProcessingResultEventFixture.rejectedEvent;
import static com.keepgoing.keepgoing.support.ImageProcessingResultEventFixture.safeEvent;
import static com.keepgoing.keepgoing.support.ImageProcessingResultEventFixture.scanningEvent;
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

import com.keepgoing.keepgoing.common.image.domain.ImageProcessingStatus;
import com.keepgoing.keepgoing.common.image.event.ImageProcessingRequestedEvent;
import com.keepgoing.keepgoing.common.image.event.ImageProcessingResultEvent;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.global.storage.InputStreamSupplier;
import com.keepgoing.keepgoing.global.storage.ObjectStorageClient;
import com.keepgoing.keepgoing.global.storage.ObjectStorageException;
import com.keepgoing.keepgoing.note.domain.Note;
import com.keepgoing.keepgoing.note.domain.NoteImage;
import com.keepgoing.keepgoing.note.domain.NoteVisibility;
import com.keepgoing.keepgoing.note.event.NoteImageProcessingRequestPublisher;
import com.keepgoing.keepgoing.note.repository.NoteImageRepository;
import com.keepgoing.keepgoing.note.repository.NoteRepository;
import com.keepgoing.keepgoing.note.service.dto.NoteImageUploadCommand;
import com.keepgoing.keepgoing.note.service.dto.NoteImageUploadResult;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
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
	private static final String STORAGE_KEY = "notes/10/generated-image-key";
	private static final Instant REQUESTED_AT = Instant.parse("2026-05-15T00:00:00Z");

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

	@Mock
	NoteImageProcessingRequestPublisher requestPublisher;

	@Mock
	Clock clock;

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
			NoteImageUploadCommand command = uploadCommand(NOTE_ID, CONTENT_TYPE);

			givenUploadAndSaveSucceed(command, note, uploader);

			// when
			NoteImageUploadResult result = noteImageService.uploadImage(UPLOADER_ID, command);

			// then
			ArgumentCaptor<NoteImage> noteImageCaptor = ArgumentCaptor.forClass(NoteImage.class);
			InOrder inOrder = inOrder(objectStorageClient, noteImageRepository, requestPublisher);
			inOrder.verify(objectStorageClient).upload(
					eq(command.inputStreamSupplier()),
					eq("notes/" + NOTE_ID),
					eq(command.fileSize())
			);
			inOrder.verify(noteImageRepository).save(noteImageCaptor.capture());
			inOrder.verify(requestPublisher).publish(any(ImageProcessingRequestedEvent.class));

			NoteImage savedImage = noteImageCaptor.getValue();
			assertThat(savedImage.getStorageKey()).isEqualTo(STORAGE_KEY);
			assertThat(savedImage.getContentType()).isEqualTo(command.contentType());
			assertThat(savedImage.getStatus()).isEqualTo(ImageProcessingStatus.PENDING);

			assertThat(result.publicId()).isEqualTo(savedImage.getPublicId());
			assertThat(result.status()).isEqualTo(savedImage.getStatus());
			verify(objectStorageClient, never()).delete(any());
		}

		@Test
		@DisplayName("업로드 성공 후 처리 요청 이벤트에 이미지 식별자와 스토리지 정보를 담아 발행한다")
		void publishesProcessingRequestWithImageMetadata() {
			// given
			Note note = persistedNote(user(UPLOADER_ID));
			User uploader = user(UPLOADER_ID);
			NoteImageUploadCommand command = uploadCommand(NOTE_ID, CONTENT_TYPE);
			givenUploadAndSaveSucceed(command, note, uploader);

			// when
			NoteImageUploadResult result = noteImageService.uploadImage(UPLOADER_ID, command);

			// then
			ArgumentCaptor<ImageProcessingRequestedEvent> eventCaptor =
					ArgumentCaptor.forClass(ImageProcessingRequestedEvent.class);
			verify(requestPublisher).publish(eventCaptor.capture());

			ImageProcessingRequestedEvent publishedEvent = eventCaptor.getValue();
			assertThat(publishedEvent.publicId()).isEqualTo(result.publicId());
			assertThat(publishedEvent.storageKey()).isEqualTo(STORAGE_KEY);
			assertThat(publishedEvent.contentType()).isEqualTo(command.contentType());
			assertThat(publishedEvent.fileSize()).isEqualTo(FILE_SIZE);
			assertThat(publishedEvent.requestedAt()).isEqualTo(REQUESTED_AT);
		}

		@Test
		@DisplayName("존재하지 않는 노트에 업로드하면 NOTE_NOT_FOUND 예외를 던지고 업로드를 시도하지 않는다")
		void throwsWhenNoteNotFound() {
			// given
			NoteImageUploadCommand command = uploadCommand(NOTE_ID, CONTENT_TYPE);
			given(noteRepository.findById(NOTE_ID)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> noteImageService.uploadImage(UPLOADER_ID, command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_NOT_FOUND);

			verify(noteRepository).findById(NOTE_ID);
			verifyNoInteractions(objectStorageClient, transactionTemplate, userRepository, noteImageRepository,
					requestPublisher);
		}

		@Test
		@DisplayName("타인 노트에 업로드하면 NOTE_ACCESS_DENIED 예외를 던지고 업로드를 시도하지 않는다")
		void throwsWhenRequesterIsNotOwner() {
			// given
			NoteImageUploadCommand command = uploadCommand(NOTE_ID, CONTENT_TYPE);
			Note otherUsersNote = persistedNote(user(2L));
			given(noteRepository.findById(NOTE_ID)).willReturn(Optional.of(otherUsersNote));

			// when & then
			assertThatThrownBy(() -> noteImageService.uploadImage(UPLOADER_ID, command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_ACCESS_DENIED);

			verify(noteRepository).findById(NOTE_ID);
			verifyNoInteractions(objectStorageClient, transactionTemplate, userRepository, noteImageRepository,
					requestPublisher);
		}

		@Test
		@DisplayName("스토리지 업로드가 실패하면 SERVICE_UNAVAILABLE 예외를 던지고 DB 저장을 시도하지 않는다")
		void throwsWhenStorageUploadFails() {
			// given
			Note note = persistedNote(user(UPLOADER_ID));
			NoteImageUploadCommand command = uploadCommand(NOTE_ID, CONTENT_TYPE);
			ObjectStorageException storageException = new ObjectStorageException("upload failed",
					new RuntimeException("io"));

			given(noteRepository.findById(NOTE_ID)).willReturn(Optional.of(note));
			given(objectStorageClient.upload(any(), eq("notes/" + NOTE_ID), eq(FILE_SIZE)))
					.willThrow(storageException);

			// when & then
			assertThatThrownBy(() -> noteImageService.uploadImage(UPLOADER_ID, command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SERVICE_UNAVAILABLE)
					.hasCause(storageException);

			verify(noteRepository).findById(NOTE_ID);
			verify(objectStorageClient).upload(any(), eq("notes/" + NOTE_ID), eq(FILE_SIZE));
			verifyNoInteractions(transactionTemplate, userRepository, noteImageRepository, requestPublisher);
			verify(objectStorageClient, never()).delete(any());
		}

		@Test
		@DisplayName("DB 저장이 실패하면 업로드된 파일을 삭제하고 원래 예외를 그대로 던진다")
		void cleansUpAndRethrowsWhenDbSaveFails() {
			// given
			Note note = persistedNote(user(UPLOADER_ID));
			User uploader = user(UPLOADER_ID);
			NoteImageUploadCommand command = uploadCommand(NOTE_ID, CONTENT_TYPE);
			RuntimeException dbException = new RuntimeException("db save failed");

			given(noteRepository.findById(NOTE_ID)).willReturn(Optional.of(note));
			given(objectStorageClient.upload(any(), eq("notes/" + NOTE_ID), eq(FILE_SIZE)))
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
			inOrder.verify(objectStorageClient)
					.upload(any(), eq("notes/" + NOTE_ID), eq(FILE_SIZE));
			inOrder.verify(noteImageRepository).save(any(NoteImage.class));
			verify(objectStorageClient).delete(STORAGE_KEY);
			verify(requestPublisher, never()).publish(any(ImageProcessingRequestedEvent.class));
		}

		@Test
		@DisplayName("처리 요청 이벤트 발행이 실패하면 업로드 파일은 삭제하지 않고 발행 예외를 던진다")
		void doesNotCleanupUploadedFileWhenPublishingRequestFails() {
			// given
			Note note = persistedNote(user(UPLOADER_ID));
			User uploader = user(UPLOADER_ID);
			NoteImageUploadCommand command = uploadCommand(NOTE_ID, CONTENT_TYPE);
			RuntimeException publishException = new RuntimeException("publish failed");

			givenUploadAndSaveSucceed(command, note, uploader);
			given(requestPublisher.publish(any(ImageProcessingRequestedEvent.class))).willThrow(publishException);

			// when & then
			assertThatThrownBy(() -> noteImageService.uploadImage(UPLOADER_ID, command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SERVICE_UNAVAILABLE);

			verify(noteImageRepository).save(any(NoteImage.class));
			verify(requestPublisher).publish(any(ImageProcessingRequestedEvent.class));
			verify(objectStorageClient, never()).delete(any());
		}

		@Test
		@DisplayName("DB 저장과 파일 삭제가 모두 실패하면 원래 예외를 던지고 삭제 실패 예외를 suppressed에 보관한다")
		void keepsSuppressedExceptionWhenCleanupAlsoFails() {
			// given
			Note note = persistedNote(user(UPLOADER_ID));
			User uploader = user(UPLOADER_ID);
			NoteImageUploadCommand command = uploadCommand(NOTE_ID, CONTENT_TYPE);
			RuntimeException dbException = new RuntimeException("db save failed");
			ObjectStorageException deleteException = new ObjectStorageException("delete failed",
					new RuntimeException("network"));

			given(noteRepository.findById(NOTE_ID)).willReturn(Optional.of(note));
			given(objectStorageClient.upload(any(), eq("notes/" + NOTE_ID), eq(FILE_SIZE)))
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
			verify(requestPublisher, never()).publish(any(ImageProcessingRequestedEvent.class));
		}
	}

	@Nested
	@DisplayName("applyProcessingResult")
	class ApplyProcessingResult {

		@Test
		@DisplayName("SCANNING 결과를 반영하면 이미지 상태를 SCANNING으로 변경한다")
		void marksImageAsScanning() {
			// given
			NoteImage noteImage = noteImage();
			ImageProcessingResultEvent event = processingResultEvent(
					noteImage.getPublicId(),
					ImageProcessingStatus.SCANNING
			);
			given(noteImageRepository.findByPublicId(noteImage.getPublicId()))
					.willReturn(Optional.of(noteImage));

			// when
			noteImageService.applyProcessingResult(event);

			// then
			assertThat(noteImage.getStatus()).isEqualTo(ImageProcessingStatus.SCANNING);
			verify(noteImageRepository).findByPublicId(noteImage.getPublicId());
		}

		@Test
		@DisplayName("SAFE 결과를 반영하면 이미지 저장 정보를 갱신하고 상태를 SAFE로 변경한다")
		void updatesImageMetadataAndMarksImageAsSafe() {
			// given
			NoteImage noteImage = noteImage();
			ImageProcessingResultEvent event = processingResultEvent(
					noteImage.getPublicId(),
					ImageProcessingStatus.SAFE
			);
			given(noteImageRepository.findByPublicId(noteImage.getPublicId()))
					.willReturn(Optional.of(noteImage));

			// when
			noteImageService.applyProcessingResult(event);

			// then
			assertThat(noteImage.getStatus()).isEqualTo(ImageProcessingStatus.SAFE);
			assertThat(noteImage.getStorageKey()).isEqualTo(event.secureStorageKey());
			assertThat(noteImage.getContentType()).isEqualTo(event.contentType());
			assertThat(noteImage.getFileSize()).isEqualTo(event.fileSize());
			verify(noteImageRepository).findByPublicId(noteImage.getPublicId());
		}

		@Test
		@DisplayName("REJECTED 결과를 반영하면 이미지 상태를 REJECTED로 변경한다")
		void marksImageAsRejected() {
			// given
			NoteImage noteImage = noteImage();
			ImageProcessingResultEvent event = processingResultEvent(
					noteImage.getPublicId(),
					ImageProcessingStatus.REJECTED
			);
			given(noteImageRepository.findByPublicId(noteImage.getPublicId()))
					.willReturn(Optional.of(noteImage));

			// when
			noteImageService.applyProcessingResult(event);

			// then
			assertThat(noteImage.getStatus()).isEqualTo(ImageProcessingStatus.REJECTED);
			verify(noteImageRepository).findByPublicId(noteImage.getPublicId());
		}

		@Test
		@DisplayName("존재하지 않는 이미지 처리 결과면 NOTE_IMAGE_NOT_FOUND 예외를 던진다")
		void throwsWhenImageNotFound() {
			// given
			UUID publicId = UUID.randomUUID();
			ImageProcessingResultEvent event = processingResultEvent(publicId, ImageProcessingStatus.SAFE);
			given(noteImageRepository.findByPublicId(publicId)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> noteImageService.applyProcessingResult(event))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_IMAGE_NOT_FOUND);

			verify(noteImageRepository).findByPublicId(publicId);
		}

		@Test
		@DisplayName("PENDING 결과는 처리 완료 결과가 아니므로 INVALID_INPUT 예외를 던진다")
		void throwsWhenResultStatusIsPending() {
			// given
			NoteImage noteImage = noteImage();
			ImageProcessingResultEvent event = processingResultEvent(
					noteImage.getPublicId(),
					ImageProcessingStatus.PENDING
			);
			given(noteImageRepository.findByPublicId(noteImage.getPublicId()))
					.willReturn(Optional.of(noteImage));

			// when & then
			assertThatThrownBy(() -> noteImageService.applyProcessingResult(event))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);

			assertThat(noteImage.getStatus()).isEqualTo(ImageProcessingStatus.PENDING);
			verify(noteImageRepository).findByPublicId(noteImage.getPublicId());
		}

		@Test
		@DisplayName("SAFE 상태 이미지에 SAFE 중복 결과가 도착하면 무시하고 변경하지 않는다")
		void ignoresDuplicateSafeResult() {
			// given
			NoteImage noteImage = noteImageWithStatus(ImageProcessingStatus.SAFE);
			ImageProcessingResultEvent event = processingResultEvent(
					noteImage.getPublicId(), ImageProcessingStatus.SAFE);
			given(noteImageRepository.findByPublicId(noteImage.getPublicId()))
					.willReturn(Optional.of(noteImage));

			// when
			noteImageService.applyProcessingResult(event);

			// then
			assertThat(noteImage.getStatus()).isEqualTo(ImageProcessingStatus.SAFE);
			verify(noteImageRepository).findByPublicId(noteImage.getPublicId());
		}

		@Test
		@DisplayName("SAFE 상태 이미지에 REJECTED 결과가 도착하면 무시하고 SAFE를 유지한다")
		void ignoresRejectedWhenAlreadySafe() {
			// given
			NoteImage noteImage = noteImageWithStatus(ImageProcessingStatus.SAFE);
			ImageProcessingResultEvent event = processingResultEvent(
					noteImage.getPublicId(), ImageProcessingStatus.REJECTED);
			given(noteImageRepository.findByPublicId(noteImage.getPublicId()))
					.willReturn(Optional.of(noteImage));

			// when
			noteImageService.applyProcessingResult(event);

			// then
			assertThat(noteImage.getStatus()).isEqualTo(ImageProcessingStatus.SAFE);
			verify(noteImageRepository).findByPublicId(noteImage.getPublicId());
		}

		@Test
		@DisplayName("REJECTED 상태 이미지에 SAFE 결과가 도착하면 무시하고 REJECTED를 유지한다")
		void ignoresSafeWhenAlreadyRejected() {
			// given
			NoteImage noteImage = noteImageWithStatus(ImageProcessingStatus.REJECTED);
			ImageProcessingResultEvent event = processingResultEvent(
					noteImage.getPublicId(), ImageProcessingStatus.SAFE);
			given(noteImageRepository.findByPublicId(noteImage.getPublicId()))
					.willReturn(Optional.of(noteImage));

			// when
			noteImageService.applyProcessingResult(event);

			// then
			assertThat(noteImage.getStatus()).isEqualTo(ImageProcessingStatus.REJECTED);
			verify(noteImageRepository).findByPublicId(noteImage.getPublicId());
		}

		@Test
		@DisplayName("REJECTED 상태 이미지에 SCANNING 결과가 도착하면 무시하고 REJECTED를 유지한다")
		void ignoresScanningWhenAlreadyRejected() {
			// given
			NoteImage noteImage = noteImageWithStatus(ImageProcessingStatus.REJECTED);
			ImageProcessingResultEvent event = processingResultEvent(
					noteImage.getPublicId(), ImageProcessingStatus.SCANNING);
			given(noteImageRepository.findByPublicId(noteImage.getPublicId()))
					.willReturn(Optional.of(noteImage));

			// when
			noteImageService.applyProcessingResult(event);

			// then
			assertThat(noteImage.getStatus()).isEqualTo(ImageProcessingStatus.REJECTED);
			verify(noteImageRepository).findByPublicId(noteImage.getPublicId());
		}
	}

	private static NoteImageUploadCommand uploadCommand(Long noteId, String contentType) {
		InputStreamSupplier supplier = () -> new ByteArrayInputStream("img".getBytes(StandardCharsets.UTF_8));
		return new NoteImageUploadCommand(
				noteId,
				supplier,
				ORIGINAL_FILE_NAME,
				contentType,
				FILE_SIZE
		);
	}

	private static Note persistedNote(User author) {
		Note note = Note.create(author, null, "제목", "본문", NoteVisibility.PRIVATE, false);
		ReflectionTestUtils.setField(note, "id", NOTE_ID);
		return note;
	}

	private static NoteImage noteImage() {
		User uploader = user(UPLOADER_ID);
		return NoteImage.create(
				persistedNote(uploader),
				uploader,
				STORAGE_KEY,
				ORIGINAL_FILE_NAME,
				CONTENT_TYPE,
				FILE_SIZE
		);
	}

	private static ImageProcessingResultEvent processingResultEvent(
			UUID publicId,
			ImageProcessingStatus status
	) {
		return switch (status) {
			case SCANNING -> scanningEvent(publicId);
			case SAFE -> safeEvent(publicId);
			case REJECTED -> rejectedEvent(publicId, "");
			case PENDING -> pendingEvent(publicId);
		};
	}

	private void givenUploadAndSaveSucceed(NoteImageUploadCommand command, Note note, User uploader) {
		given(noteRepository.findById(command.noteId())).willReturn(Optional.of(note));
		given(objectStorageClient.upload(any(), eq("notes/" + command.noteId()), eq(command.fileSize())))
				.willReturn(STORAGE_KEY);
		given(transactionTemplate.execute(any())).willAnswer(invocation -> {
			TransactionCallback<NoteImageUploadResult> callback = invocation.getArgument(0);
			return callback.doInTransaction(mock(TransactionStatus.class));
		});
		given(userRepository.getReferenceById(UPLOADER_ID)).willReturn(uploader);
		given(noteImageRepository.save(any(NoteImage.class))).willAnswer(invocation -> invocation.getArgument(0));
		given(clock.instant()).willReturn(REQUESTED_AT);
	}

	private static NoteImage noteImageWithStatus(ImageProcessingStatus status) {
		NoteImage image = noteImage();
		ReflectionTestUtils.setField(image, "status", status);
		return image;
	}
}
