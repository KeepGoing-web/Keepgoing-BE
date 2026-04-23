package com.keepgoing.keepgoing.ai.controller.dto;

import com.keepgoing.keepgoing.ai.service.dto.AiPanelMessageResult;

public record AiPanelMessageResponse(
		String assistantMessage,
		Long contextNoteId,
		boolean contextAttached
) {
	public static AiPanelMessageResponse from(AiPanelMessageResult result) {
		return new AiPanelMessageResponse(
				result.assistantMessage(),
				result.contextNoteId(),
				result.contextAttached()
		);
	}
}
