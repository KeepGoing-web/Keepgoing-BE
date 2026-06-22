package com.keepgoing.keepgoing.ai.repository;

import java.time.LocalDateTime;

public interface AiNoteIndexingSourceView {

	Long getNoteId();

	Long getAuthorId();

	String getTitle();

	String getContent();

	boolean isAiCollectable();

	LocalDateTime getDeletedAt();

	LocalDateTime getUpdatedAt();
}
