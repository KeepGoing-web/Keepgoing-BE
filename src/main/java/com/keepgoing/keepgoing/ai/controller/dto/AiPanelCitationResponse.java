package com.keepgoing.keepgoing.ai.controller.dto;

import com.keepgoing.keepgoing.ai.service.dto.AiPanelCitationResult;

public record AiPanelCitationResponse(
		Long noteId,
		String title,
		String excerpt,
		String sourceType
) {
	public static AiPanelCitationResponse from(AiPanelCitationResult result) {
		return new AiPanelCitationResponse(
				result.noteId(),
				result.title(),
				result.excerpt(),
				result.sourceType().name()
		);
	}
}
