package com.keepgoing.keepgoing.auth.controller;

import com.keepgoing.keepgoing.auth.controller.dto.LoginRequest;
import com.keepgoing.keepgoing.auth.controller.dto.LoginResponse;
import com.keepgoing.keepgoing.auth.controller.dto.SignupRequest;
import com.keepgoing.keepgoing.auth.controller.dto.SignupResponse;
import com.keepgoing.keepgoing.global.api.response.ApiResponse;
import com.keepgoing.keepgoing.global.api.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Auth", description = "인증/회원 관련 API")
@RequestMapping("/api/auth")
public interface AuthApiDocs {

    @Operation(summary = "회원가입")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "회원가입 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "입력값 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 가입된 이메일",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "UserAlreadyExists",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "USER_ALREADY_EXISTS",
                                                "message": "이미 가입된 이메일입니다."
                                              }
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/signup")
    ResponseEntity<ApiResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest request
    );


    @Operation(summary = "로그인")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "입력값 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (이메일 없음 또는 비밀번호 불일치)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "AuthenticationFailed",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "AUTH_INVALID_CREDENTIALS",
                                                "message": "이메일 또는 비밀번호가 올바르지 않습니다."
                                              }
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/login")
    ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse servletResponse
    );

    @Operation(summary = "Access Token 재발급")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "재발급 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "리프레시 토큰이 없거나 유효하지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "RefreshTokenInvalid",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "AUTH_REFRESH_TOKEN_INVALID",
                                                "message": "유효하지 않은 리프레시 토큰입니다."
                                              }
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/refresh")
    ResponseEntity<ApiResponse<Void>> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    );

    @Operation(summary = "로그아웃")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 성공"
            )
    })
    @PostMapping("/logout")
    ResponseEntity<ApiResponse<Void>> logout(
            HttpServletResponse response
    );
}
