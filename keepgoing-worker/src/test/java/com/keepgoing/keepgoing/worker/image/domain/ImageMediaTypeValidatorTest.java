package com.keepgoing.keepgoing.worker.image.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ImageMediaTypeValidatorTest {

	private static final String CONTENT_TYPE_JPEG = "image/jpeg";
	private static final String CONTENT_TYPE_PNG = "image/png";
	private static final String CONTENT_TYPE_WEBP = "image/webp";

	@ParameterizedTest(name = "{0}")
	@MethodSource("matchingImageContentTypes")
	@DisplayName("요청 MIME과 Tika 감지 MIME이 같은 이미지이면 검증에 성공한다")
	void validateWhenRequestedAndDetectedImageContentTypeMatch(
			String name,
			byte[] bytes,
			String requestedContentType,
			String expectedDetectedContentType
	) {
		// when
		ImageValidationResult result =
				ImageMediaTypeValidator.validate(bytes, requestedContentType);

		// then
		assertThat(result.valid()).isTrue();
		assertThat(result.detectedContentType()).isEqualTo(expectedDetectedContentType);
		assertThat(result.reason()).isNull();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("nonImageRequestedContentTypes")
	@DisplayName("요청 MIME이 image/*가 아니면 검증에 실패한다")
	void invalidateWhenRequestedContentTypeIsNotImage(
			String name,
			byte[] bytes,
			String requestedContentType,
			String expectedDetectedContentType
	) {
		// when
		ImageValidationResult result =
				ImageMediaTypeValidator.validate(bytes, requestedContentType);

		// then
		assertThat(result.valid()).isFalse();
		assertThat(result.detectedContentType()).isEqualTo(expectedDetectedContentType);
		assertThat(result.reason())
				.isEqualTo(ImageValidationFailureReason.UNSUPPORTED_CONTENT_TYPE);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("nonImageDetectedContentTypes")
	@DisplayName("Tika가 실제 파일을 image/*로 감지하지 못하면 검증에 실패한다")
	void invalidateWhenDetectedContentTypeIsNotImage(
			String name,
			byte[] bytes,
			String requestedContentType
	) {
		// when
		ImageValidationResult result =
				ImageMediaTypeValidator.validate(bytes, requestedContentType);

		// then
		assertThat(result.valid()).isFalse();
		assertThat(result.detectedContentType()).isNull();
		assertThat(result.reason())
				.isEqualTo(ImageValidationFailureReason.UNSUPPORTED_IMAGE_SIGNATURE);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("mismatchedImageContentTypes")
	@DisplayName("요청 MIME과 Tika 감지 MIME이 모두 이미지지만 서로 다르면 검증에 실패한다")
	void invalidateWhenRequestedAndDetectedImageContentTypeMismatch(
			String name,
			byte[] bytes,
			String requestedContentType,
			String expectedDetectedContentType
	) {
		// when
		ImageValidationResult result =
				ImageMediaTypeValidator.validate(bytes, requestedContentType);

		// then
		assertThat(result.valid()).isFalse();
		assertThat(result.detectedContentType()).isEqualTo(expectedDetectedContentType);
		assertThat(result.reason())
				.isEqualTo(ImageValidationFailureReason.CONTENT_TYPE_MISMATCH);
	}

	static Stream<Arguments> matchingImageContentTypes() {
		return Stream.of(
				Arguments.of("JPEG", jpegBytes(), CONTENT_TYPE_JPEG, CONTENT_TYPE_JPEG),
				Arguments.of("PNG", pngBytes(), CONTENT_TYPE_PNG, CONTENT_TYPE_PNG),
				Arguments.of("WebP", webpBytes(), CONTENT_TYPE_WEBP, CONTENT_TYPE_WEBP),
				Arguments.of("요청 MIME 대소문자를 정규화한다", pngBytes(), "image/PNG", CONTENT_TYPE_PNG)
		);
	}

	static Stream<Arguments> nonImageRequestedContentTypes() {
		return Stream.of(
				Arguments.of("null MIME", pngBytes(), null, CONTENT_TYPE_PNG),
				Arguments.of("empty MIME", pngBytes(), "", CONTENT_TYPE_PNG),
				Arguments.of("blank MIME", pngBytes(), "   ", CONTENT_TYPE_PNG),
				Arguments.of("text/plain", pngBytes(), "text/plain", CONTENT_TYPE_PNG),
				Arguments.of(
						"application/octet-stream",
						pngBytes(),
						"application/octet-stream",
						CONTENT_TYPE_PNG
				)
		);
	}

	static Stream<Arguments> nonImageDetectedContentTypes() {
		return Stream.of(
				Arguments.of(
						"텍스트 파일",
						"not-image".getBytes(StandardCharsets.UTF_8),
						CONTENT_TYPE_PNG
				),
				Arguments.of(
						"빈 파일",
						new byte[]{},
						CONTENT_TYPE_PNG
				),
				Arguments.of(
						"JPEG signature 일부만 존재",
						new byte[]{(byte) 0xFF},
						CONTENT_TYPE_JPEG
				),
				Arguments.of(
						"PNG signature 일부만 존재",
						new byte[]{(byte) 0x89, 0x50, 0x4E},
						CONTENT_TYPE_PNG
				),
				Arguments.of(
						"WebP RIFF만 있고 WEBP 식별자가 없음",
						new byte[]{'R', 'I', 'F', 'F'},
						CONTENT_TYPE_WEBP
				)
		);
	}

	static Stream<Arguments> mismatchedImageContentTypes() {
		return Stream.of(
				Arguments.of("JPEG 파일인데 image/png로 요청", jpegBytes(), CONTENT_TYPE_PNG, CONTENT_TYPE_JPEG),
				Arguments.of("PNG 파일인데 image/jpeg로 요청", pngBytes(), CONTENT_TYPE_JPEG, CONTENT_TYPE_PNG),
				Arguments.of("WebP 파일인데 image/png로 요청", webpBytes(), CONTENT_TYPE_PNG, CONTENT_TYPE_WEBP),

				// 새 정책에서는 image/gif, image/jpg 자체가 unsupported가 아니라 image/* mismatch다.
				Arguments.of("PNG 파일인데 image/gif로 요청", pngBytes(), "image/gif", CONTENT_TYPE_PNG),
				Arguments.of("PNG 파일인데 image/jpg로 요청", pngBytes(), "image/jpg", CONTENT_TYPE_PNG)
		);
	}

	private static byte[] jpegBytes() {
		return new byte[]{
				(byte) 0xFF, (byte) 0xD8, (byte) 0xFF,
				0x00, 0x00, 0x00
		};
	}

	private static byte[] pngBytes() {
		return new byte[]{
				(byte) 0x89, 0x50, 0x4E, 0x47,
				0x0D, 0x0A, 0x1A, 0x0A,
				0x00, 0x00
		};
	}

	private static byte[] webpBytes() {
		return new byte[]{
				'R', 'I', 'F', 'F',
				0x00, 0x00, 0x00, 0x00,
				'W', 'E', 'B', 'P',
				0x00, 0x00
		};
	}
}
