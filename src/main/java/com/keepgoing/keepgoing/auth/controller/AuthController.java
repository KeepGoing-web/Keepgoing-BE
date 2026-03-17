package com.keepgoing.keepgoing.auth.controller;

import com.keepgoing.keepgoing.auth.AuthMapper;
import com.keepgoing.keepgoing.auth.controller.dto.LoginRequest;
import com.keepgoing.keepgoing.auth.controller.dto.LoginResponse;
import com.keepgoing.keepgoing.auth.controller.dto.MyInfoResponse;
import com.keepgoing.keepgoing.auth.controller.dto.SignupRequest;
import com.keepgoing.keepgoing.auth.controller.dto.SignupResponse;
import com.keepgoing.keepgoing.auth.service.AuthService;
import com.keepgoing.keepgoing.auth.service.dto.LoginCommand;
import com.keepgoing.keepgoing.auth.service.dto.LoginResult;
import com.keepgoing.keepgoing.auth.service.dto.MyInfoResult;
import com.keepgoing.keepgoing.auth.service.dto.SignupCommand;
import com.keepgoing.keepgoing.auth.service.dto.SignupResult;
import com.keepgoing.keepgoing.global.api.response.ApiResponse;
import com.keepgoing.keepgoing.global.security.cookie.AuthCookieManager;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApiDocs {

	private final AuthService authService;
	private final AuthMapper authMapper;
	private final AuthCookieManager authCookieManager;

	@PostMapping("/signup")
	public ResponseEntity<ApiResponse<SignupResponse>> signup(
			@Valid @RequestBody SignupRequest request
	) {
		SignupCommand command = authMapper.toCommand(request);
		SignupResult result = authService.signup(command);
		SignupResponse response = authMapper.toResponse(result);

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success(response));
	}

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<LoginResponse>> login(
			@Valid @RequestBody LoginRequest request,
			HttpServletResponse servletResponse
	) {
		LoginCommand command = authMapper.toCommand(request);
		LoginResult result = authService.login(command);

		authCookieManager.addAccessToken(servletResponse, result.accessToken());
		authCookieManager.addRefreshToken(servletResponse, result.refreshToken());

		LoginResponse body = authMapper.toResponse(result);

		return ResponseEntity.status(HttpStatus.OK)
				.body(ApiResponse.success(body));
	}

	@GetMapping("/me")
	public ResponseEntity<ApiResponse<MyInfoResponse>> getMyInfo(
			@AuthenticationPrincipal Long userId
	) {
		MyInfoResult result = authService.getMyInfo(userId);

		MyInfoResponse body = authMapper.toResponse(result);

		return ResponseEntity.status(HttpStatus.OK)
				.body(ApiResponse.success(body));
	}
}
