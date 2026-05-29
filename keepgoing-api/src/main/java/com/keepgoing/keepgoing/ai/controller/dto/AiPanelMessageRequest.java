package com.keepgoing.keepgoing.ai.controller.dto;

import com.keepgoing.keepgoing.ai.service.dto.AiPanelMessageCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiPanelMessageRequest(
		Long contextNoteId,

		@NotBlank
		@Size(max = MAX_MESSAGE_LENGTH)
		String message
) {
	public static final int MAX_MESSAGE_LENGTH = 10_000;

	public AiPanelMessageCommand toCommand() {
		return new AiPanelMessageCommand(
				contextNoteId,
				message
		);
	}
}
