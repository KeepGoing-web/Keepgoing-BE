package com.keepgoing.keepgoing.global.common.error;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * ErrorCode에 HttpStatus를 포함하는 이유
 * <p>
 * - Service 계층이 BusinessException 생성 시 HttpStatus를 직접 다루면 Web 계층(HTTP)에 대한 의존성이 생김. - 이를 피하기 위해 HttpStatus를 ErrorCode에
 * 포함시켜, Service는 ErrorCode만 사용하고 Web 계층에서만 HttpStatus를 참조하도록 설계. - 향후 gRPC 등 다른 표현 계층에서는 이 필드를 무시하는 전략을 사용할 수 있음.
 */
@Schema(description = "에러 코드")
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// ===== Auth =====
	AUTH_INVALID_CREDENTIALS(
			HttpStatus.UNAUTHORIZED,
			"AUTH_001",
			"이메일 또는 비밀번호가 올바르지 않습니다."
	),
	AUTH_TOKEN_EXPIRED(
			HttpStatus.UNAUTHORIZED,
			"AUTH_002",
			"인증 토큰이 만료되었습니다."
	),
	AUTH_TOKEN_INVALID(
			HttpStatus.UNAUTHORIZED,
			"AUTH_003",
			"유효하지 않은 인증 토큰입니다."
	),
	AUTH_TOKEN_MISSING(
			HttpStatus.UNAUTHORIZED,
			"AUTH_004",
			"인증 토큰이 존재하지 않습니다."
	),
	AUTH_REFRESH_TOKEN_INVALID(
			HttpStatus.UNAUTHORIZED,
			"AUTH_005",
			"유효하지 않은 리프레시 토큰입니다."
	),
	AUTH_REFRESH_TOKEN_EXPIRED(
			HttpStatus.UNAUTHORIZED,
			"AUTH_006",
			"리프레시 토큰이 만료되었습니다."
	),
	AUTH_EMAIL_NOT_VERIFIED(
			HttpStatus.FORBIDDEN,
			"AUTH_007",
			"이메일 인증이 완료되지 않았습니다."
	),

	// ===== OAuth =====
	OAUTH_PROVIDER_ERROR(
			HttpStatus.INTERNAL_SERVER_ERROR,
			"OAUTH_001",
			"Provider 통신 실패했습니다."
	),
	OAUTH_ACCOUNT_ALREADY_LINK(
			HttpStatus.CONFLICT,
			"OAUTH_002",
			"이미 다른 User에 연동된 계정입니다."
	),
	OAUTH_PROVIDER_NOT_SUPPORTED(
			HttpStatus.BAD_REQUEST,
			"OAUTH_003",
			"지원하지 않은 Provider입니다."
	),

	// ===== User =====
	USER_NOT_FOUND(
			HttpStatus.NOT_FOUND,
			"USER_001",
			"사용자를 찾을 수 없습니다."
	),
	USER_ALREADY_EXISTS(
			HttpStatus.CONFLICT,
			"USER_002",
			"이미 가입된 이메일입니다."
	),
	USER_SUSPENDED(
			HttpStatus.FORBIDDEN,
			"USER_003",
			"정지된 사용자입니다."
	),
	USER_DELETED(
			HttpStatus.FORBIDDEN,
			"USER_004",
			"삭제된 사용자입니다."
	),

	// ===== Validation =====
	VALIDATION_FAILED(
			HttpStatus.BAD_REQUEST,
			"VALID_001",
			"입력 값 검증에 실패했습니다."
	),
	INVALID_INPUT(
			HttpStatus.BAD_REQUEST,
			"VALID_002",
			"유효하지 않은 입력 값입니다."
	),
	INVALID_PASSWORD_FORMAT(
			HttpStatus.BAD_REQUEST,
			"VALID_003",
			"비밀번호 형식이 올바르지 않습니다."
	),
	PASSWORD_TOO_WEAK(
			HttpStatus.BAD_REQUEST,
			"VALID_004",
			"비밀번호가 너무 약합니다."
	),

	// ===== Permission =====
	PERMISSION_DENIED(
			HttpStatus.FORBIDDEN,
			"PERM_001",
			"권한이 없습니다."
	),
	ROLE_INSUFFICIENT(
			HttpStatus.FORBIDDEN,
			"PERM_002",
			"요청을 수행하기에 권한이 부족합니다."
	),

	// ===== Rate limit =====
	RATE_LIMIT_EXCEEDED(
			HttpStatus.TOO_MANY_REQUESTS,
			"RATE_001",
			"요청 한도를 초과했습니다."
	),

	// ===== System / 공통 =====
	INTERNAL_SERVER_ERROR(
			HttpStatus.INTERNAL_SERVER_ERROR,
			"SYS_001",
			"알 수 없는 서버 오류가 발생했습니다."
	),
	SERVICE_UNAVAILABLE(
			HttpStatus.SERVICE_UNAVAILABLE,
			"SYS_002",
			"일시적으로 서비스를 사용할 수 없습니다."
	),
	RESOURCE_NOT_FOUND(
			HttpStatus.NOT_FOUND,
			"SYS_003",
			"요청한 리소스를 찾을 수 없습니다."
	),


	// ===== NOTE =====
	NOTE_NOT_FOUND(
			HttpStatus.NOT_FOUND,
			"NOTE_01",
			"글을 찾을 수 없습니다."
	),
	NOTE_ACCESS_DENIED(
			HttpStatus.FORBIDDEN,
			"NOTE_02",
			"해당 게시글에 대한 권한이 없습니다."
	),
	NOTE_SEARCH_KEYWORD_REQUIRED(
			HttpStatus.BAD_REQUEST,
			"NOTE_03",
			"검색어(keyword)는 필수입니다."
	);

	private final HttpStatus httpStatus;
	private final String code;        // 시스템 내부/프론트에서 쓰는 에러 코드
	private final String defaultMessage;     // 기본 메시지 (한국어)
}