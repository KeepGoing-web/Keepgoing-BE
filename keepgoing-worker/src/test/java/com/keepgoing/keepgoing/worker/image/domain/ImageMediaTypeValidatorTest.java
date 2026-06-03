package com.keepgoing.keepgoing.worker.image.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ImageSignatureValidatorTest {

	public static final String CONTENT_TYPE_JPEG = "image/jpeg";
	public static final String CONTENT_TYPE_PNG = "image/PNG";
	public static final String CONTENT_TYPE_WEBP = "image/webp";


	@ParameterizedTest(name = "{0}")
	@MethodSource("validImages")
	@DisplayName("허용된 이미지 signature와 MIME이 일치하면 검증에 성공한다.")
	void validateSupportedImageSignatures(
			String name,
			byte[] bytes,
			String contentType
	) {
		// when
		ImageValidationResult result =
				ImageSignatureValidator.validate(bytes, contentType);

		assertThat(result).isNotNull();
		assertThat(result.valid()).isTrue();
		assertThat(result.detectedContentType()).isEqualTo(contentType);
		assertThat(result.reason()).isNull();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("mismatchedContentTypes")
	@DisplayName("실제 signature와 요청 MIME이 다르면 검증에 실패한다.")
	void invalidateSupportedImageSignatures(
			String name,
			byte[] bytes,
			String requestedContentType,
			String expectedDetectedContentType
	) {
		// when
		ImageValidationResult result =
				ImageSignatureValidator.validate(bytes, requestedContentType);

		assertThat(result).isNotNull();
		assertThat(result.valid()).isFalse();
		assertThat(result.detectedContentType()).isEqualTo(expectedDetectedContentType);
		assertThat(result.reason())
				.isEqualTo(ImageValidationFailureReason.CONTENT_TYPE_MISMATCH);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("unsupportedSignatures")
	@DisplayName("지원하지 않는 signature이면 검증에 실패한다.")
	void invalidateWhenSignatureIsUnsupported(
			String name,
			byte[] bytes,
			String requestedContentType
	) {
		// when
		ImageValidationResult result =
				ImageSignatureValidator.validate(bytes, requestedContentType);

		assertThat(result).isNotNull();
		assertThat(result.valid()).isFalse();
		assertThat(result.detectedContentType()).isNull();
		assertThat(result.reason())
				.isEqualTo(ImageValidationFailureReason.UNSUPPORTED_IMAGE_SIGNATURE);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("unsupportedContentTypes")
	@DisplayName("지원하지 않는 MIME이면 signature가 맞아도 검증에 실패한다")
	void invalidateWhenContentTypeIsUnsupported(
			String name,
			byte[] bytes,
			String requestedContentType,
			String expectedDetectedContentType
	) {
		// when
		ImageValidationResult result =
				ImageSignatureValidator.validate(bytes, requestedContentType);

		// then
		assertThat(result).isNotNull();
		assertThat(result.valid()).isFalse();
		assertThat(result.detectedContentType()).isEqualTo(expectedDetectedContentType);
		assertThat(result.reason())
				.isEqualTo(ImageValidationFailureReason.UNSUPPORTED_CONTENT_TYPE);
	}

	static Stream<Arguments> validImages() {
		return Stream.of(
				Arguments.of("JPEG", jpegBytes(), CONTENT_TYPE_JPEG, CONTENT_TYPE_JPEG),
				Arguments.of("PNG", pngBytes(), CONTENT_TYPE_PNG, CONTENT_TYPE_PNG),
				Arguments.of("WebP", webpBytes(), CONTENT_TYPE_WEBP, CONTENT_TYPE_WEBP),

				// MIME 대소문자를 허용할 거라면 이 케이스도 유지
				Arguments.of("PNG MIME 대소문자 혼합", pngBytes(), "image/PNG", CONTENT_TYPE_PNG)
		);
	}

	static Stream<Arguments> mismatchedContentTypes() {
		return Stream.of(
				Arguments.of("JPEG 파일인데 image/png로 요청", jpegBytes(), CONTENT_TYPE_PNG, CONTENT_TYPE_JPEG),
				Arguments.of("PNG 파일인데 image/webp로 요청", pngBytes(), CONTENT_TYPE_WEBP, CONTENT_TYPE_PNG),
				Arguments.of("WebP 파일인데 image/jpeg로 요청", webpBytes(), CONTENT_TYPE_JPEG,
						CONTENT_TYPE_WEBP)
		);
	}

	static Stream<Arguments> unsupportedSignatures() {
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

	static Stream<Arguments> unsupportedContentTypes() {
		return Stream.of(
				Arguments.of("null MIME", pngBytes(), null, CONTENT_TYPE_PNG),
				Arguments.of("empty MIME", pngBytes(), "", CONTENT_TYPE_PNG),
				Arguments.of("blank MIME", pngBytes(), "   ", CONTENT_TYPE_PNG),
				Arguments.of("text/plain", pngBytes(), "text/plain", CONTENT_TYPE_PNG),
				Arguments.of("image/gif", pngBytes(), "image/gif", CONTENT_TYPE_PNG),
				Arguments.of("image/jpg", pngBytes(), "image/jpg", CONTENT_TYPE_PNG),
				Arguments.of("application/octet-stream", pngBytes(), "application/octet-stream",
						CONTENT_TYPE_PNG)
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