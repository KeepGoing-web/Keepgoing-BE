package com.keepgoing.keepgoing.ai.service.dto;

import java.util.List;

public record AiPanelMessageResult(
		String assistantMessage,
		Long contextNoteId,
		boolean contextAttached,
		List<AiPanelCitationResult> citations
) {
}