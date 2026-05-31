package com.keepgoing.keepgoing.ai.service.dto;

public record AiPanelCitationResult(
		Long noteId,
		String title,
		String excerpt,
		AiCitationSourceType sourceType
) {
}
