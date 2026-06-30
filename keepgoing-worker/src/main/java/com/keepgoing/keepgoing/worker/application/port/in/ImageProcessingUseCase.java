package com.keepgoing.keepgoing.worker.application.port.in;

/**
 * Worker가 수신한 이미지 처리 요청을 애플리케이션 계층으로 전달하는 인바운드 포트.
 *
 * <p> 요청 출처와 결과 발행 방식은 어댑터 및 구현체가 담당하며,
 * 호출자는 {@link ImageProcessingCommand}에 처리 대상 이미지의 식별자, 격리 저장소 키, 요청 메타데이터를 담아 전달한다.</p>
 */
public interface ImageProcessingUseCase {

	/**
	 * 이미지 처리 요청을 실행한다.
	 *
	 * <p>처리 결과는 반환값이 아니라 구현체가 발행하는 결과 이벤트를 통해 전달된다.</p>
	 *
	 * @param command 처리 대상 이미지와 요청 메타데이터
	 */
	void process(ImageProcessingCommand command);
}
