package com.keepgoing.keepgoing.worker.domain;

import static org.assertj.core.api.Assertions.assertThat;

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
	@DisplayName("요청 MIME과 감지 MIME이 같은 이미지이면 검증에 성공한다")
	void validateWhenRequestedAndDetectedImageContentTypeMatch(
			String name,
			String requestedContentType,
			String detectedContentType,
			String expectedDetectedContentType
	) {
		// when
		ImageValidationResult result =
				ImageMediaTypeValidator.validate(requestedContentType, detectedContentType);

		// then
		assertThat(result.valid()).isTrue();
		assertThat(result.detectedContentType()).isEqualTo(expectedDetectedContentType);
		assertThat(result.reason()).isNull();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("unsupportedRequestedContentTypes")
	@DisplayName("요청 MIME이 허용 목록에 없으면 검증에 실패한다")
	void invalidateWhenRequestedContentTypeIsNotImage(
			String name,
			String requestedContentType,
			String detectedContentType,
			String expectedDetectedContentType
	) {
		// when
		ImageValidationResult result =
				ImageMediaTypeValidator.validate(requestedContentType, detectedContentType);

		// then
		assertThat(result.valid()).isFalse();
		assertThat(result.detectedContentType()).isEqualTo(expectedDetectedContentType);
		assertThat(result.reason())
				.isEqualTo(ImageValidationFailureReason.UNSUPPORTED_CONTENT_TYPE);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("unsupportedDetectedContentTypes")
	@DisplayName("감지 MIME이 허용 목록에 없으면 검증에 실패한다")
	void invalidateWhenDetectedContentTypeIsNotImage(
			String name,
			String requestedContentType,
			String detectedContentType,
			String expectedDetectedContentType
	) {
		// when
		ImageValidationResult result =
				ImageMediaTypeValidator.validate(requestedContentType, detectedContentType);

		// then
		assertThat(result.valid()).isFalse();
		assertThat(result.detectedContentType()).isEqualTo(expectedDetectedContentType);
		assertThat(result.reason())
				.isEqualTo(ImageValidationFailureReason.UNSUPPORTED_IMAGE_SIGNATURE);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("mismatchedImageContentTypes")
	@DisplayName("요청 MIME과 감지 MIME이 모두 이미지지만 서로 다르면 검증에 실패한다")
	void invalidateWhenRequestedAndDetectedImageContentTypeMismatch(
			String name,
			String requestedContentType,
			String detectedContentType,
			String expectedDetectedContentType
	) {
		// when
		ImageValidationResult result =
				ImageMediaTypeValidator.validate(requestedContentType, detectedContentType);

		// then
		assertThat(result.valid()).isFalse();
		assertThat(result.detectedContentType()).isEqualTo(expectedDetectedContentType);
		assertThat(result.reason())
				.isEqualTo(ImageValidationFailureReason.CONTENT_TYPE_MISMATCH);
	}

	static Stream<Arguments> matchingImageContentTypes() {
		return Stream.of(
				Arguments.of("JPEG", CONTENT_TYPE_JPEG, CONTENT_TYPE_JPEG,
						CONTENT_TYPE_JPEG),
				Arguments.of("PNG", CONTENT_TYPE_PNG, CONTENT_TYPE_PNG, CONTENT_TYPE_PNG),
				Arguments.of("WebP", CONTENT_TYPE_WEBP, CONTENT_TYPE_WEBP,
						CONTENT_TYPE_WEBP),
				Arguments.of("요청 MIME 대소문자를 정규화한다", "image/PNG",
						CONTENT_TYPE_PNG, CONTENT_TYPE_PNG),
				Arguments.of("감지 MIME 대소문자를 정규화한다", CONTENT_TYPE_PNG, "image/PNG", CONTENT_TYPE_PNG)
		);
	}

	static Stream<Arguments> unsupportedRequestedContentTypes() {
		return Stream.of(
				Arguments.of("null MIME", null, CONTENT_TYPE_PNG, CONTENT_TYPE_PNG),
				Arguments.of("empty MIME", "", CONTENT_TYPE_PNG, CONTENT_TYPE_PNG),
				Arguments.of("blank MIME", "   ", CONTENT_TYPE_PNG, CONTENT_TYPE_PNG),
				Arguments.of("text/plain", "text/plain", CONTENT_TYPE_PNG,
						CONTENT_TYPE_PNG),
				Arguments.of("application/octet-stream", "application/octet-stream",
						CONTENT_TYPE_PNG, CONTENT_TYPE_PNG),
				Arguments.of("image/gif", "image/gif", CONTENT_TYPE_PNG, CONTENT_TYPE_PNG),
				Arguments.of("image/jpg", "image/jpg", CONTENT_TYPE_PNG, CONTENT_TYPE_PNG),
				Arguments.of("image/svg+xml", "image/svg+xml", CONTENT_TYPE_PNG,
						CONTENT_TYPE_PNG),
				Arguments.of("image/avif", "image/avif", CONTENT_TYPE_PNG, CONTENT_TYPE_PNG)
		);
	}

	static Stream<Arguments> unsupportedDetectedContentTypes() {
		return Stream.of(
				Arguments.of("null 감지 MIME", CONTENT_TYPE_PNG, null, null),
				Arguments.of("text/plain", CONTENT_TYPE_PNG, "text/plain", "text/plain"),
				Arguments.of("application/octet-stream", CONTENT_TYPE_PNG, "application/octet-stream",
						"application/octet-stream"),
				Arguments.of("image/gif", CONTENT_TYPE_PNG, "image/gif", "image/gif")
		);
	}

	static Stream<Arguments> mismatchedImageContentTypes() {
		return Stream.of(
				Arguments.of("JPEG 요청인데 PNG 감지", CONTENT_TYPE_JPEG, CONTENT_TYPE_PNG,
						CONTENT_TYPE_PNG),
				Arguments.of("PNG 요청인데 JPEG 감지", CONTENT_TYPE_PNG, CONTENT_TYPE_JPEG,
						CONTENT_TYPE_JPEG),
				Arguments.of("WebP 요청인데 PNG 감지", CONTENT_TYPE_WEBP, CONTENT_TYPE_PNG,
						CONTENT_TYPE_PNG)
		);
	}
}
