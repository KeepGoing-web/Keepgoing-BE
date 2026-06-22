package com.keepgoing.keepgoing.ai.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.keepgoing.keepgoing.ai.service.dto.AiPanelMessageResult;
import java.util.List;

@JsonInclude(Include.NON_NULL)
public record AiPanelMessageResponse(
		String assistantMessage,
		Long contextNoteId,
		boolean contextAttached,
		List<AiPanelCitationResponse> citations
) {
	public static AiPanelMessageResponse from(AiPanelMessageResult result) {
		List<AiPanelCitationResponse> citations = result.citations() == null
				? List.of()
				: result.citations().stream()
						.map(AiPanelCitationResponse::from)
						.toList();

		return new AiPanelMessageResponse(
				result.assistantMessage(),
				result.contextNoteId(),
				result.contextAttached(),
				citations
		);
	}
}
