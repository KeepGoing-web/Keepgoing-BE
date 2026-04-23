package com.keepgoing.keepgoing.ai.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.keepgoing.keepgoing.ai.service.dto.AiPanelMessageResult;

@JsonInclude(Include.NON_NULL)
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
