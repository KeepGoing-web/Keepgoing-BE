package com.keepgoing.keepgoing.worker.domain;

public enum ImageValidationFailureReason {
	UNSUPPORTED_CONTENT_TYPE,       // 요청 MIME이 아예 이미지가 아님
	UNSUPPORTED_IMAGE_SIGNATURE,    // Tika가 실제 파일을 이미지로 감지하지 못함
	CONTENT_TYPE_MISMATCH,          // 요청 MIME과 실제 감지 MIME이 서로 다름
	SANITIZATION_FAILED
}
