package com.keepgoing.keepgoing.ai.controller.dto;

import com.keepgoing.keepgoing.ai.service.dto.AiPanelMessageCommand;
import jakarta.validation.constraints.NotBlank;

public record AiPanelMessageRequest(
		Long contextNoteId,

		@NotBlank
		String message
) {
	public AiPanelMessageCommand toCommand() {
		return new AiPanelMessageCommand(
				contextNoteId,
				message
		);
	}
}
