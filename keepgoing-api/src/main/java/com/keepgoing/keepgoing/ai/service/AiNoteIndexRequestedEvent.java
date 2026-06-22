package com.keepgoing.keepgoing.ai.service;

public record AiNoteIndexRequestedEvent(
		Long noteId,
		Long authorId
) {
}
