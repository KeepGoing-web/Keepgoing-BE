package com.keepgoing.keepgoing.note.service;

import com.keepgoing.keepgoing.common.image.domain.ImageProcessingStatus;
import com.keepgoing.keepgoing.common.image.event.ImageProcessingRequestedEvent;
import com.keepgoing.keepgoing.common.image.event.ImageProcessingResultEvent;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.global.storage.ObjectStorageClient;
import com.keepgoing.keepgoing.global.storage.ObjectStorageException;
import com.keepgoing.keepgoing.global.storage.StorageProperties;
import com.keepgoing.keepgoing.note.domain.Note;
import com.keepgoing.keepgoing.note.domain.NoteImage;
import com.keepgoing.keepgoing.note.domain.NoteVisibility;
import com.keepgoing.keepgoing.note.event.NoteImageProcessingRequestPublisher;
import com.keepgoing.keepgoing.note.repository.NoteImageRepository;
import com.keepgoing.keepgoing.note.repository.NoteRepository;
import com.keepgoing.keepgoing.note.service.dto.NoteImageDeleteCommand;
import com.keepgoing.keepgoing.note.service.dto.NoteImagePresignQuery;
import com.keepgoing.keepgoing.note.service.dto.NoteImageUploadCommand;
import com.keepgoing.keepgoing.note.service.dto.NoteImageUploadResult;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoteImageService {

	private final Clock clock;
	private final UserRepository userRepository;
	private final NoteRepository noteRepository;
	private final NoteImageRepository noteImageRepository;
	private final ObjectStorageClient objectStorageClient;
	private final TransactionTemplate transactionTemplate;
	private final NoteImageProcessingRequestPublisher requestPublisher;
	private final StorageProperties storageProperties;

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
		Optional<NoteImage> optional = noteImageRepository.findByPublicId(event.publicId());
		if (optional.isEmpty()) {
			if (event.status() == ImageProcessingStatus.SAFE && event.secureStorageKey() != null) {
				objectStorageClient.delete(
						storageProperties.bucketNames().secure(),
						event.secureStorageKey()
				);
			}
			log.info("이미지가 삭제되었으므로 스킵: {}", event.publicId());
			return;
		}

		NoteImage noteImage = optional.orElseThrow(() -> new BusinessException(ErrorCode.NOTE_IMAGE_NOT_FOUND));
		if (noteImage.getStatus().isTerminal()) {
			log.info("Skipped processing result for image {}: already {}", event.publicId(), noteImage.getStatus());
			return;
		}

		switch (event.status()) {
			case SAFE -> noteImage.markSafe(event.secureStorageKey(), event.contentType(), event.fileSize());
			case SCANNING -> noteImage.markScanning();
			case REJECTED -> noteImage.markRejected();
			case PENDING -> throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
	}

	@Transactional(readOnly = true)
	public String getPresignedUrl(NoteImagePresignQuery query) {
		Long userId = query.userId();
		NoteImage noteImage = noteImageRepository.findByPublicIdAndNote_Id(query.publicId(), query.noteId())
				.orElseThrow(() -> new BusinessException(ErrorCode.NOTE_IMAGE_NOT_FOUND));
		Note note = noteImage.getNote();

		if (!note.isAuthor(userId) && note.getVisibility() != NoteVisibility.PUBLIC) {
			throw new BusinessException(ErrorCode.NOTE_IMAGE_NOT_FOUND);
		}

		if (noteImage.getStatus() != ImageProcessingStatus.SAFE) {
			throw new BusinessException(ErrorCode.NOTE_IMAGE_NOT_FOUND);
		}

		Duration duration = note.isAuthor(userId)
				? Duration.ofMinutes(15)
				: Duration.ofHours(1);

		return objectStorageClient.generatePresignedUrl(
				storageProperties.bucketNames().secure(),
				noteImage.getStorageKey(),
				duration
		);
	}

	/*
		Hard delete 진행.
		이는 사용자가 명시적으로 삭제하기 때문에 Hard delete가 적절하다고 판단.
	 */
	@Transactional
	public void deleteImage(NoteImageDeleteCommand command) {
		NoteImage noteImage = noteImageRepository.findByPublicIdAndNote_Id(command.publicId(), command.noteId())
				.orElseThrow(() -> new BusinessException(ErrorCode.NOTE_IMAGE_NOT_FOUND));

		noteImage.validateUploader(command.userId());

		switch (noteImage.getStatus()) {
			case PENDING, SCANNING -> objectStorageClient.delete(
					storageProperties.bucketNames().quarantine(),
					noteImage.getStorageKey()
			);
			case SAFE -> objectStorageClient.delete(
					storageProperties.bucketNames().secure(),
					noteImage.getStorageKey()
			);
		}
		noteImageRepository.delete(noteImage);
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
