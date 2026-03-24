package com.keepgoing.keepgoing.folder.controller;

import com.keepgoing.keepgoing.folder.controller.dto.CreateFolderRequest;
import com.keepgoing.keepgoing.folder.controller.dto.CreateFolderResponse;
import com.keepgoing.keepgoing.folder.service.FolderService;
import com.keepgoing.keepgoing.folder.service.dto.CreateFolderCommand;
import com.keepgoing.keepgoing.folder.service.dto.FolderResult;
import com.keepgoing.keepgoing.global.api.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/folders")
public class FolderController {

	private final FolderService folderService;

	@PostMapping
	public ResponseEntity<ApiResponse<CreateFolderResponse>> createFolder(
			@AuthenticationPrincipal Long userId,
			@Valid @RequestBody CreateFolderRequest request
	) {
		CreateFolderCommand command = request.toCommand(userId);
		FolderResult result = folderService.createFolder(command);
		CreateFolderResponse response = CreateFolderResponse.from(result);

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success(response));
	}
}
