package com.keepgoing.keepgoing.ai.controller;

import com.keepgoing.keepgoing.ai.controller.dto.AiPanelMessageRequest;
import com.keepgoing.keepgoing.ai.controller.dto.AiPanelMessageResponse;
import com.keepgoing.keepgoing.ai.service.dto.AiPanelMessageResult;
import com.keepgoing.keepgoing.ai.service.AiPanelService;
import com.keepgoing.keepgoing.ai.service.dto.AiPanelMessageCommand;
import com.keepgoing.keepgoing.global.api.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai/panel")
public class AiPanelController {

	private final AiPanelService aiPanelService;

	@PostMapping("/messages")
	public ResponseEntity<ApiResponse<AiPanelMessageResponse>> chatAi(
			@Valid @RequestBody AiPanelMessageRequest request,
			@AuthenticationPrincipal Long userId
	) {
		AiPanelMessageCommand command = request.toCommand();
		AiPanelMessageResult result = aiPanelService.sendMessage(userId, command);
		AiPanelMessageResponse response = AiPanelMessageResponse.from(result);

		return ResponseEntity.ok(ApiResponse.success(response));
	}
}
