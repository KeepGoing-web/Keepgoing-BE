package com.keepgoing.keepgoing.worker.image.application.port.out;

import com.keepgoing.keepgoing.common.image.event.ImageProcessingResultEvent;

/**
 * 이미지 처리 결과를 외부 시스템으로 발행하는 아웃바운드 포트.
 *
 * <p>애플리케이션 계층은 이 포트를 통해 처리 상태와 실패 사유를 알리며,
 * 구체적인 전달 방식은 어댑터 구현체가 담당한다.</p>
 */
public interface ImageProcessingResultPublisherPort {

	/**
	 * 이미지 처리 결과 이벤트를 발행한다.
	 *
	 * @param event 처리 대상 이미지의 식별자, 처리 상태, 실패 사유, 처리 시각을 담은 이벤트
	 */
	void publish(ImageProcessingResultEvent event);
}
