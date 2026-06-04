package com.keepgoing.keepgoing.common.image.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ImageContentTypePolicyTest {

	@ParameterizedTest(name = "{0}")
	@MethodSource("allowedContentTypes")
	@DisplayName("허용된 이미지 MIME이면 통과한다")
	void isAllowedReturnsTrueForAllowedContentType(
			String name,
			String contentType
	) {
		// when
		boolean result = ImageContentTypePolicy.isAllowed(contentType);

		// then
		assertThat(result).isTrue();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("unsupportedContentTypes")
	@DisplayName("허용 목록에 없는 MIME이면 거부한다")
	void isAllowedReturnsFalseForUnsupportedContentType(
			String name,
			String contentType
	) {
		// when
		boolean result = ImageContentTypePolicy.isAllowed(contentType);

		// then
		assertThat(result).isFalse();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("normalizedContentTypes")
	@DisplayName("MIME 정규화는 앞뒤 공백 제거와 소문자 변환만 수행한다")
	void normalizeTrimsAndLowercasesOnly(
			String name,
			String contentType,
			String expected
	) {
		// when
		String result = ImageContentTypePolicy.normalize(contentType);

		// then
		assertThat(result).isEqualTo(expected);
	}

	@Test
	@DisplayName("허용 MIME 목록은 JPEG, PNG, WebP만 포함한다")
	void allowedContentTypesContainsOnlySupportedMimeTypes() {
		assertThat(ImageContentTypePolicy.allowedContentTypes())
				.containsExactlyInAnyOrder(
						"image/jpeg",
						"image/png",
						"image/webp"
				);
	}

	static Stream<Arguments> allowedContentTypes() {
		return Stream.of(
				Arguments.of("JPEG", "image/jpeg"),
				Arguments.of("PNG", "image/png"),
				Arguments.of("WebP", "image/webp"),
				Arguments.of("대소문자 정규화", "IMAGE/JPEG"),
				Arguments.of("앞뒤 공백 정규화", " image/png ")
		);
	}

	static Stream<Arguments> unsupportedContentTypes() {
		return Stream.of(
				Arguments.of("null", null),
				Arguments.of("empty", ""),
				Arguments.of("blank", "  "),
				Arguments.of("비표준 image/jpg", "image/jpg"),
				Arguments.of("GIF", "image/gif"),
				Arguments.of("SVG", "image/svg+xml"),
				Arguments.of("AVIF", "image/avif"),
				Arguments.of("text/plain", "text/plain"),
				Arguments.of("파라미터가 붙은 MIME", "image/jpeg; charset=utf-8")
		);
	}

	static Stream<Arguments> normalizedContentTypes() {
		return Stream.of(
				Arguments.of("null은 null 유지", null, null),
				Arguments.of("소문자는 그대로 유지", "image/jpeg", "image/jpeg"),
				Arguments.of("대문자는 소문자로 변환", "IMAGE/PNG", "image/png"),
				Arguments.of("앞뒤 공백 제거", " image/webp ", "image/webp"),
				Arguments.of("image/jpg는 image/jpeg로 변환하지 않음", "image/jpg", "image/jpg"),
				Arguments.of(
						"파라미터가 붙은 MIME은 제거하지 않음",
						"image/jpeg; charset=utf-8",
						"image/jpeg; charset=utf-8"
				)
		);
	}
}