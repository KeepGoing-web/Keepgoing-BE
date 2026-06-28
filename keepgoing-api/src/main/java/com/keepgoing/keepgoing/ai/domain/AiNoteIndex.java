package com.keepgoing.keepgoing.ai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_note_indexes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiNoteIndex {

	private static final int MAX_ERROR_LENGTH = 1000;

	@Id
	@Column(name = "note_id", nullable = false, updatable = false)
	private Long noteId;

	@Column(name = "author_id", nullable = false)
	private Long authorId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private AiNoteIndexStatus status;

	@Column(name = "attempt_count", nullable = false)
	private int attemptCount;

	@Column(name = "last_requested_at", nullable = false)
	private LocalDateTime lastRequestedAt;

	@Column(name = "last_processed_at")
	private LocalDateTime lastProcessedAt;

	@Column(name = "indexed_at")
	private LocalDateTime indexedAt;

	@Column(name = "last_error", length = MAX_ERROR_LENGTH)
	private String lastError;

	private AiNoteIndex(Long noteId, Long authorId, LocalDateTime requestedAt) {
		this.noteId = noteId;
		this.authorId = authorId;
		this.status = AiNoteIndexStatus.PENDING;
		this.lastRequestedAt = requestedAt;
	}

	public static AiNoteIndex pending(Long noteId, Long authorId, LocalDateTime requestedAt) {
		return new AiNoteIndex(noteId, authorId, requestedAt);
	}

	public void markPending(Long authorId, LocalDateTime requestedAt) {
		this.authorId = authorId;
		this.status = AiNoteIndexStatus.PENDING;
		this.lastRequestedAt = requestedAt;
		this.lastError = null;
		this.attemptCount = 0;
	}

	public void markCompleted(LocalDateTime processedAt) {
		this.status = AiNoteIndexStatus.COMPLETED;
		this.lastProcessedAt = processedAt;
		this.indexedAt = processedAt;
		this.lastError = null;
		this.attemptCount++;
	}

	public void markRemoved(LocalDateTime processedAt) {
		this.status = AiNoteIndexStatus.REMOVED;
		this.lastProcessedAt = processedAt;
		this.indexedAt = null;
		this.lastError = null;
		this.attemptCount++;
	}

	public void markFailed(LocalDateTime processedAt, String errorMessage) {
		this.status = AiNoteIndexStatus.FAILED;
		this.lastProcessedAt = processedAt;
		this.lastError = truncate(errorMessage);
		this.attemptCount++;
	}

	private String truncate(String errorMessage) {
		if (errorMessage == null || errorMessage.length() <= MAX_ERROR_LENGTH) {
			return errorMessage;
		}
		return errorMessage.substring(0, MAX_ERROR_LENGTH);
	}
}
