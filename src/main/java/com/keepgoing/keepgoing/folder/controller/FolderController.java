package com.keepgoing.keepgoing.folder.controller;

import com.keepgoing.keepgoing.folder.controller.dto.FolderCreateRequest;
import com.keepgoing.keepgoing.folder.controller.dto.FolderRenameRequest;
import com.keepgoing.keepgoing.folder.controller.dto.FolderSummaryResponse;
import com.keepgoing.keepgoing.folder.controller.dto.FolderTreeNodeResponse;
import com.keepgoing.keepgoing.folder.service.FolderService;
import com.keepgoing.keepgoing.folder.service.dto.FolderCreateCommand;
import com.keepgoing.keepgoing.folder.service.dto.FolderRenameCommand;
import com.keepgoing.keepgoing.folder.service.dto.FolderSummaryResult;
import com.keepgoing.keepgoing.folder.service.dto.FolderTreeNodeResult;
import com.keepgoing.keepgoing.global.api.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/folders")
public class FolderController {

	private final FolderService folderService;

	@PostMapping
	public ResponseEntity<ApiResponse<FolderSummaryResponse>> createFolder(
			@AuthenticationPrincipal Long userId,
			@Valid @RequestBody FolderCreateRequest request
	) {
		FolderCreateCommand command = request.toCommand(userId);
		FolderSummaryResult result = folderService.createFolder(command);
		FolderSummaryResponse response = FolderSummaryResponse.from(result);

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success(response));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<FolderSummaryResponse>>> getFolders(
			@RequestParam(required = false) Long parentId,
			@AuthenticationPrincipal Long userId
	) {
		List<FolderSummaryResult> results = folderService.getFolders(userId, parentId);

		List<FolderSummaryResponse> responses = results.stream()
				.map(FolderSummaryResponse::from)
				.toList();

		return ResponseEntity.ok(ApiResponse.success(responses));
	}

	@GetMapping("/tree")
	public ResponseEntity<ApiResponse<List<FolderTreeNodeResponse>>> getFolderTree(
			@AuthenticationPrincipal Long userId
	) {
		List<FolderTreeNodeResult> results = folderService.getFolderTree(userId);

		List<FolderTreeNodeResponse> responses = results.stream()
				.map(FolderTreeNodeResponse::from)
				.toList();

		return ResponseEntity.ok(ApiResponse.success(responses));
	}
}
