package com.keepgoing.keepgoing.user.controller;

import com.keepgoing.keepgoing.user.controller.dto.UserInfoResponse;
import com.keepgoing.keepgoing.user.controller.dto.UserUpdateRequest;
import com.keepgoing.keepgoing.user.service.dto.UserInfoResult;
import com.keepgoing.keepgoing.global.api.response.ApiResponse;
import com.keepgoing.keepgoing.user.service.UserService;
import com.keepgoing.keepgoing.user.service.dto.UserUpdateCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping("/me")
	public ResponseEntity<ApiResponse<UserInfoResponse>> getMyInfo(
			@AuthenticationPrincipal Long userId
	) {
		UserInfoResult result = userService.getMyInfo(userId);

		return ResponseEntity.status(HttpStatus.OK)
				.body(ApiResponse.success(UserInfoResponse.from(result)));
	}

	@PatchMapping("/me")
	public ResponseEntity<ApiResponse<UserInfoResponse>> updateMyProfile(
			@AuthenticationPrincipal Long userId,
			@Valid @RequestBody UserUpdateRequest request
	) {
		UserUpdateCommand command = request.toCommand(userId);
		UserInfoResult result = userService.updateMyProfile(command);

		return ResponseEntity.status(HttpStatus.OK)
				.body(ApiResponse.success(UserInfoResponse.from(result)));
	}
}
