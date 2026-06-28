package com.keepgoing.keepgoing.worker.infrastructure.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;

import com.keepgoing.keepgoing.worker.support.ImageFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TikaImageMediaTypeDetectorAdapterTest {

	private final TikaImageMediaTypeDetectorAdapter detector
			= new TikaImageMediaTypeDetectorAdapter();

	@Test
	@DisplayName("bytes가 null이면 null을 반환한다")
	void detect_nullBytes_returnsNull() {
		// given & when
		String detect = detector.detect(null);

		// then
		assertThat(detect).isNull();
	}

	@Test
	@DisplayName("bytes가 비어있으면 null을 반환한다")
	void detect_emptyBytes_returnsNull() {
		// given
		byte[] bytes = new byte[0];

		// when
		String detect = detector.detect(bytes);

		// then
		assertThat(detect).isNull();
	}

	@Test
	@DisplayName("유효한 PNG 바이트에 대해 image/png를 반환한다")
	void detect_validPngBytes_returnsImagePng() {
		byte[] imageBytes = ImageFixture.pngBytes();

		// when
		String detect = detector.detect(imageBytes);

		// then
		assertThat(detect).isEqualTo("image/png");
	}
}