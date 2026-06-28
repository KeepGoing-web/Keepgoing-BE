package com.keepgoing.keepgoing.note.service;

import com.keepgoing.keepgoing.common.image.event.ImageProcessingRequestedEvent;
import com.keepgoing.keepgoing.common.image.event.ImageProcessingResultEvent;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.global.storage.ObjectStorageClient;
import com.keepgoing.keepgoing.global.storage.ObjectStorageException;
import com.keepgoing.keepgoing.note.domain.Note;
import com.keepgoing.keepgoing.note.domain.NoteImage;
import com.keepgoing.keepgoing.note.event.NoteImageProcessingRequestPublisher;
import com.keepgoing.keepgoing.note.repository.NoteImageRepository;
import com.keepgoing.keepgoing.note.repository.NoteRepository;
import com.keepgoing.keepgoing.note.service.dto.NoteImageUploadCommand;
import com.keepgoing.keepgoing.note.service.dto.NoteImageUploadResult;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class NoteImageService {

	private final Clock clock;
	private final UserRepository userRepository;
	private final NoteRepository noteRepository;
	private final NoteImageRepository noteImageRepository;
	private final ObjectStorageClient objectStorageClient;
	private final TransactionTemplate transactionTemplate;
	private final NoteImageProcessingRequestPublisher requestPublisher;

	public NoteImageUploadResult uploadImage(Long uploaderId, NoteImageUploadCommand command) {
		Long noteId = command.noteId();
		Note note = findNoteAndValidateOwner(noteId, uploaderId);

		String storageKey = uploadToStorage(noteId, command);

		NoteImageUploadResult result;
		try {
			result = saveNoteImageInfo(uploaderId, command, note, storageKey);
		} catch (RuntimeException e) {
			cleanupUploadedFile(e, storageKey);
			throw e;
		}

		publishProcessingRequest(result.publicId(), command.contentType(), command.fileSize(), storageKey);

		return result;
	}

	@Transactional
	public void applyProcessingResult(ImageProcessingResultEvent event) {
		NoteImage noteImage = noteImageRepository.findByPublicId(event.publicId())
				.orElseThrow(() -> new BusinessException(ErrorCode.NOTE_IMAGE_NOT_FOUND));

		switch (event.status()) {
			case SAFE -> noteImage.markSafe(event.secureStorageKey(), event.contentType(), event.fileSize());
			case SCANNING -> noteImage.markScanning();
			case REJECTED -> noteImage.markRejected();
			case PENDING -> throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
	}

	private void publishProcessingRequest(
			UUID publicId,
			String contentType,
			long fileSize,
			String storageKey
	) {
		try {
			requestPublisher.publish(new ImageProcessingRequestedEvent(
					publicId,
					storageKey,
					contentType,
					fileSize,
					Instant.now(clock),
					0
			));
		} catch (RuntimeException e) {
			throw new BusinessException(
					ErrorCode.SERVICE_UNAVAILABLE,
					"이미지 처리 요청 이벤트 발행에 실패했습니다.",
					e
			);
		}
	}

	private NoteImageUploadResult saveNoteImageInfo(
			Long uploaderId,
			NoteImageUploadCommand command,
			Note note,
			String storageKey
	) {
		return transactionTemplate.execute(
				status -> {
					User uploader = userRepository.getReferenceById(uploaderId);
					NoteImage noteImage = NoteImage.create(
							note,
							uploader,
							storageKey,
							command.originalFileName(),
							command.contentType(),
							command.fileSize()
					);
					NoteImage savedNoteImage = noteImageRepository.save(noteImage);

					return new NoteImageUploadResult(savedNoteImage.getPublicId(), savedNoteImage.getStatus());
				});
	}

	private String uploadToStorage(Long noteId, NoteImageUploadCommand command) {
		try {
			return objectStorageClient.upload(
					command.inputStreamSupplier(),
					"notes/" + noteId,
					command.fileSize()
			);
		} catch (ObjectStorageException e) {
			throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "이미지 저장소 업로드에 실패했습니다.", e);
		}
	}

	private Note findNoteAndValidateOwner(Long noteId, Long uploaderId) {
		Note note = noteRepository.findById(noteId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));

		note.validateOwner(uploaderId);
		return note;
	}

	private void cleanupUploadedFile(RuntimeException dbException, String storageKey) {
		try {
			objectStorageClient.delete(storageKey);
		} catch (ObjectStorageException deleteException) {
			dbException.addSuppressed(deleteException);
		}
	}
}
