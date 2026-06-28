package com.keepgoing.keepgoing.note.controller;

import com.keepgoing.keepgoing.global.api.response.ApiResponse;
import com.keepgoing.keepgoing.note.controller.dto.request.NoteImageUploadRequest;
import com.keepgoing.keepgoing.note.controller.dto.response.NoteImageUploadResponse;
import com.keepgoing.keepgoing.note.service.NoteImageService;
import com.keepgoing.keepgoing.note.service.dto.NoteImagePresignQuery;
import com.keepgoing.keepgoing.note.service.dto.NoteImageUploadCommand;
import com.keepgoing.keepgoing.note.service.dto.NoteImageUploadResult;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notes")
public class NoteImageController {

	private final NoteImageService noteImageService;

	@PostMapping(
			value = "/{noteId}/images"
			, consumes = MediaType.MULTIPART_FORM_DATA_VALUE
	)
	public ResponseEntity<ApiResponse<NoteImageUploadResponse>> uploadImage(
			@PathVariable Long noteId,
			@Valid @ModelAttribute NoteImageUploadRequest request,
			@AuthenticationPrincipal Long userId
	) {
		NoteImageUploadCommand command = request.toCommand(noteId);

		NoteImageUploadResult result = noteImageService.uploadImage(userId, command);

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success(NoteImageUploadResponse.from(result)));
	}

	@GetMapping("/{noteId}/images/{publicId}")
	public ResponseEntity<Void> redirectToImage(
			@PathVariable Long noteId,
			@PathVariable UUID publicId,
			@AuthenticationPrincipal Long userId
	) {
		NoteImagePresignQuery query = new NoteImagePresignQuery(userId, noteId, publicId);

		String presignedUrl = noteImageService.getPresignedUrl(query);

		return ResponseEntity.status(HttpStatus.FOUND)
				.location(URI.create(presignedUrl))
				.cacheControl(CacheControl.noCache())
				.build();
	}
}
