package com.keepgoing.keepgoing.ai.service.dto;

public record AiPanelMessageResult(
		String assistantMessage,
		Long contextNoteId,
		boolean contextAttached
) {
}