package com.keepgoing.keepgoing.worker.image.application.port.out;

public interface ImageStoragePort {

	/**
	 * quarantine bucket에 있는 object를 읽는다.
	 *
	 * @param storageKey API가 업로드 후 이벤트로 전달한 object key
	 * @return object bytes
	 * @throws ImageStorageException storage 접근/읽기 실패 시
	 */
	byte[] readQuarantineObject(String storageKey);

	/**
	 * quarantine bucket에 있는 object를 삭제한다.
	 *
	 * <p>검증 실패로 REJECTED 처리 할 때 cleanup 용도로 사용한다.</p>
	 *
	 * @param storageKey 삭제할 object key
	 * @throws ImageStorageException storage 삭제 실패 시
	 */
	void deleteQuarantineObject(String storageKey);
}
