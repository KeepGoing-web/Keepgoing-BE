package com.keepgoing.keepgoing.global.storage;

import java.time.Duration;

public interface ObjectStorageClient {

	/**
	 * 파일을 저장소에 업로드
	 *
	 * @param inputStreamSupplier 업로드할 파일 스트림 공급자
	 * @param directory           저장할 디렉토리 경로
	 * @param fileSize            파일 길이
	 * @return 저장소에 기록된 고유 식별자 (storageKey)
	 */
	String upload(
			InputStreamSupplier inputStreamSupplier,
			String directory,
			long fileSize
	);

	void delete(String storageKey);

	/**
	 * 지정된 버킷의 객체에 대한 Presigned GET URL을 생성한다.
	 *
	 * @param bucketName 버킷 이름
	 * @param key        객체 키 (storageKey)
	 * @param duration   URL 만료 시간
	 * @return Presigned GET URL
	 */
	String generatePresignedUrl(String bucketName, String key, Duration duration);
}
