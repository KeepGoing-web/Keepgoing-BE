package com.keepgoing.keepgoing.global.storage;

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

}
