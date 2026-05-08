package com.keepgoing.keepgoing.note.domain;

import com.keepgoing.keepgoing.global.common.entity.BaseEntity;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "note_images")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@SQLRestriction("deleted_at IS NULL")
public class NoteImage extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "public_id", nullable = false, unique = true, updatable = false)
	private UUID publicId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "note_id", nullable = false)
	private Note note;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "uploader_id", nullable = false)
	private User uploader;

	@Column(name = "storage_key", length = 512, unique = true, nullable = false)
	private String storageKey;

	@Column(name = "original_name", length = 255, nullable = false)
	private String originalName;

	@Column(name = "content_type", length = 100, nullable = false)
	private String contentType;

	@Column(name = "file_size", nullable = false)
	private Long fileSize;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	public static NoteImage create(
			Note note,
			User uploader,
			String storageKey,
			String originalName,
			String contentType,
			Long fileSize
	) {
		if (note == null || note.getId() == null || note.isDeleted()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}

		if (uploader == null || uploader.getId() == null) {
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}

		if (!note.isAuthor(uploader.getId())) {
			throw new BusinessException(ErrorCode.NOTE_IMAGE_ACCESS_DENIED);
		}

		if (storageKey == null || storageKey.isBlank() || storageKey.length() > 512) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}

		if (originalName == null || originalName.isBlank() || originalName.length() > 255) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}

		if (contentType == null || contentType.isBlank() || contentType.length() > 100) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}

		if (fileSize == null || fileSize <= 0) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}

		return new NoteImage(
				null,
				UUID.randomUUID(),
				note,
				uploader,
				storageKey,
				originalName,
				contentType,
				fileSize,
				null
		);
	}

	public void validateUploader(Long userId) {
		if (!isUploader(userId)) {
			throw new BusinessException(ErrorCode.NOTE_IMAGE_ACCESS_DENIED);
		}
	}

	public boolean isUploader(Long userId) {
		return this.uploader != null
				&& this.uploader.getId() != null
				&& this.uploader.getId().equals(userId);
	}

	public void softDelete() {
		if (this.deletedAt == null) {
			this.deletedAt = LocalDateTime.now();
		}
	}
}
