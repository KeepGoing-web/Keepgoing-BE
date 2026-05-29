package com.keepgoing.keepgoing.common.image.domain;

public enum ImageProcessingStatus {
	/*
	 * 격리(Quarantine) 상태. 파일이 업로드되어 버킷에 저장되었으나,
	 * 아직 보안 검증을 거치지 않음. 외부 노출이 절대 불가능한 상태.
	 */
	PENDING,

	/*
	 * 검사 중. Keepgoing-Worker가 파일을 인계받아 매직 넘버 체크,
	 * 이미지 재인코딩(Sanitization), 바이러스 스캔을 수행 중인 상태.
	 */
	SCANNING,

	/*
	 * 안전(Safe) 상태. 보안 검증 및 정제가 완료되어 'secure' 버킷으로 이동됨.
	 * 이제 사용자에게 정상적으로 노출될 수 있는 상태.
	 */
	SAFE,

	/*
	 * 거부(Rejected) 상태. 악성 코드 발견, 파손된 이미지 구조,
	 * 혹은 보안 정책 위반으로 검증에 실패한 상태. 파일은 즉시 삭제됨.
	 */
	REJECTED
}
